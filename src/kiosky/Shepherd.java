package kiosky;

import android.content.Context;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for a Kiosky Shepherd management server (docs/PROTOCOL.md in kiosky-shepherd).
 *
 * Network calls run one at a time on a background thread; everything that touches
 * the activity is posted back to the main thread through {@link Host}.
 */
final class Shepherd {

    /** What the activity provides. Every method is called on the main thread. */
    interface Host {
        /** Free-form status report for check-in (§4.1). */
        JSONObject status();
        /** A new or changed policy has been stored and must be applied (§5). */
        void onPolicy(Policy policy);
        /** The server offers an app update (§6). */
        void onUpdateOffer(JSONObject update);
        /** Runs one command and returns {status, message?, data?, screenshot?} (§7). */
        JSONObject runCommand(String type, JSONObject params);
        /** Restarts the activity, after the restart_app result has been sent. */
        void restartApp();
        /** The server retired this device; managed state is already cleared (§8). */
        void onRetired(Policy lastPolicy);
        void onEnrolled(Policy policy, String name);
        void onEnrollFailed(String message);
    }

    private static final int MAX_EVENTS_PER_CHECKIN = 50;
    private static final long FIRST_BACKOFF_MS = 15000L;

    private final Context context;
    private final Host host;
    private final ManagedState state;
    private final Handler main = new Handler();
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private boolean inFlight;
    private boolean stopped;
    private long backoffMs;
    private int intervalSeconds = 60;

    /** Shown in the unlocked toolbar and device info; null while all is well. */
    String lastError;
    long lastCheckinAt;

    private final Runnable tick = new Runnable() {
        public void run() {
            checkinNow();
        }
    };

    Shepherd(Context context, Host host, ManagedState state) {
        this.context = context;
        this.host = host;
        this.state = state;
    }

    void start() {
        if (state.isManaged()) checkinNow();
    }

    void stop() {
        stopped = true;
        main.removeCallbacks(tick);
        net.shutdown();
    }

    /** Check in shortly, e.g. after commands finish or the network comes back. */
    void checkinSoon() {
        if (stopped || !state.isManaged()) return;
        main.removeCallbacks(tick);
        main.postDelayed(tick, 1000L);
    }

    void onNetworkBack() {
        if (backoffMs > 0 || lastError != null) checkinSoon();
    }

    // ---- Enrollment (§3) -----------------------------------------------------

    void enroll(final String server, final String code) {
        final JSONObject body = Json.obj("token", code, "device", DeviceInfo.device(context));
        net.execute(new Runnable() {
            public void run() {
                final Resp r = call("POST", server, "/api/v1/device/enroll", body, null);
                main.post(new Runnable() {
                    public void run() {
                        onEnrollResponse(server, r);
                    }
                });
            }
        });
    }

    private void onEnrollResponse(String server, Resp r) {
        if (r.code == 201 && r.json != null && r.json.optJSONObject("policy") != null) {
            JSONObject policy = r.json.optJSONObject("policy");
            String name = Json.str(r.json, "name", "");
            state.enrolled(server, Json.str(r.json, "device_token", null),
                    Json.str(r.json, "device_id", ""), name, policy.toString());
            Policy p = new Policy(policy);
            intervalSeconds = r.json.optInt("checkin_seconds", p.checkinSeconds);
            host.onEnrolled(p, name);
            checkinNow(); // reports the applied policy straight away
        } else if (r.code == 403) {
            host.onEnrollFailed("That code is wrong, used up or expired. Ask your administrator for a new one.");
        } else if (r.code == 429) {
            host.onEnrollFailed("Too many attempts. Wait a few minutes and try again.");
        } else if (r.code == 0) {
            host.onEnrollFailed("Couldn't reach the server: " + r.message);
        } else {
            host.onEnrollFailed("The server refused: " + r.message);
        }
    }

    // ---- Check-in (§4) -------------------------------------------------------

    void checkinNow() {
        if (stopped || inFlight || !state.isManaged()) return;
        main.removeCallbacks(tick);
        inFlight = true;

        final JSONArray events = state.peekEvents(MAX_EVENTS_PER_CHECKIN);
        final JSONObject body = Json.obj(
                "app_version", Updater.currentVersion(context),
                "app_version_code", Updater.currentVersionCode(context),
                "policy_version", state.appliedVersion(),
                "status", host.status(),
                "events", events);
        final String server = state.server();
        final String token = state.token();

        net.execute(new Runnable() {
            public void run() {
                resendResults(server, token); // results that didn't get through last time
                final Resp r = call("POST", server, "/api/v1/device/checkin", body, token);
                main.post(new Runnable() {
                    public void run() {
                        inFlight = false;
                        if (!stopped) onCheckin(r, events.length());
                    }
                });
            }
        });
    }

    private void onCheckin(Resp r, int sentEvents) {
        if (r.code == 410) {
            retire();
            return;
        }
        if (r.code != 200 || r.json == null) {
            if (r.code == 401) {
                lastError = "Can't reach management server (this tablet wasn't recognised)";
            } else if (r.code == 429) {
                lastError = "Can't reach management server (too many requests)";
            } else if (r.code == 0) {
                lastError = "Can't reach management server: " + r.message;
            } else {
                lastError = "Can't reach management server (error " + r.code + ")";
            }
            scheduleRetry(r.code == 429);
            return;
        }

        state.dropEvents(sentEvents);
        backoffMs = 0;
        lastError = null;
        lastCheckinAt = System.currentTimeMillis();

        // 1. Policy: store first, then apply
        JSONObject policy = r.json.optJSONObject("policy");
        if (policy != null) {
            String version = Json.str(policy, "version", "");
            Policy cached = Policy.parse(state.policyJson());
            if (cached == null || !version.equals(cached.version) || !version.equals(state.appliedVersion())) {
                state.savePolicy(policy.toString());
                host.onPolicy(new Policy(policy));
            }
        }
        // 2. Name
        JSONObject device = r.json.optJSONObject("device");
        if (device != null) state.setName(Json.str(device, "name", null));
        // 3. Update
        JSONObject update = r.json.optJSONObject("update");
        if (update != null) host.onUpdateOffer(update);
        // 4. Commands
        JSONArray commands = r.json.optJSONArray("commands");
        boolean restarting = commands != null && commands.length() > 0 && runCommands(commands);
        // 5. Next check-in
        intervalSeconds = Json.clamp(r.json.optInt("checkin_seconds", intervalSeconds), 15, 3600);
        if (!restarting) main.postDelayed(tick, intervalSeconds * 1000L);
    }

    /** §4.4: 15 s, doubling up to checkin_seconds × 10 (at most 10 minutes), ±20 % jitter. */
    private void scheduleRetry(boolean rateLimited) {
        long cap = Math.min(intervalSeconds * 10000L, 600000L);
        backoffMs = backoffMs == 0 ? FIRST_BACKOFF_MS : Math.min(backoffMs * 2, cap);
        long delay = (long) (backoffMs * (0.8 + 0.4 * random.nextDouble()));
        if (rateLimited) delay = Math.max(delay, 60000L);
        main.postDelayed(tick, delay);
    }

    private void retire() {
        Policy last = Policy.parse(state.policyJson());
        state.clear();
        main.removeCallbacks(tick);
        backoffMs = 0;
        lastError = null;
        host.onRetired(last);
    }

    // ---- Commands (§7) -------------------------------------------------------

    /** Runs commands in order. Returns true if the app is about to restart. */
    private boolean runCommands(JSONArray commands) {
        final String server = state.server();
        final String token = state.token();
        for (int i = 0; i < commands.length(); i++) {
            JSONObject c = commands.optJSONObject(i);
            if (c == null) continue;
            final long id = c.optLong("id", -1);
            if (id < 0) continue;
            String type = Json.str(c, "type", "");

            JSONObject done = state.done(id);
            if (done != null) {
                // Delivered again: re-send the result without running it twice
                final JSONObject result = done.optJSONObject("result");
                net.execute(new Runnable() {
                    public void run() {
                        sendResult(server, token, id, result);
                    }
                });
                continue;
            }

            if ("restart_app".equals(type)) {
                final JSONObject result = Json.obj("status", "done", "message", "restarting");
                state.putDone(id, result, false);
                net.execute(new Runnable() {
                    public void run() {
                        sendResult(server, token, id, result); // report first, then restart
                        main.post(new Runnable() {
                            public void run() {
                                host.restartApp();
                            }
                        });
                    }
                });
                return true; // later commands are re-delivered after the restart
            }

            JSONObject params = c.optJSONObject("params");
            JSONObject result;
            try {
                result = host.runCommand(type, params == null ? new JSONObject() : params);
            } catch (Exception e) {
                result = Json.obj("status", "failed", "message", "error: " + e);
            }
            // Screenshots are too big to keep. If this send fails, the stored result
            // re-sent later says so, rather than silently taking a new picture.
            final JSONObject toSend = result;
            JSONObject toKeep = result.has("screenshot") || result.has("bundle")
                    ? Json.obj("status", "failed", "message", "the result didn't reach the server; send the command again")
                    : result;
            state.putDone(id, toKeep, false);
            net.execute(new Runnable() {
                public void run() {
                    sendResult(server, token, id, toSend);
                }
            });
        }
        // Check in straight after finishing commands (queued behind the results)
        net.execute(new Runnable() {
            public void run() {
                main.post(new Runnable() {
                    public void run() {
                        checkinSoon();
                    }
                });
            }
        });
        return false;
    }

    /** Called on the main thread with the HTTP status of a live-view frame (0: network error). */
    interface FrameSent {
        void onSent(int code);
    }

    /** One live-view frame (§7 live_view): POST /api/v1/device/live. */
    void sendLiveFrame(final String base64, final FrameSent done) {
        final String server = state.server();
        final String token = state.token();
        if (stopped || server == null || token == null) {
            done.onSent(410);
            return;
        }
        net.execute(new Runnable() {
            public void run() {
                final Resp r = call("POST", server, "/api/v1/device/live", Json.obj("screenshot", base64), token);
                main.post(new Runnable() {
                    public void run() {
                        done.onSent(r.code);
                    }
                });
            }
        });
    }

    /** Background thread. Marks the result sent once the server has it. */
    private void sendResult(String server, String token, long id, JSONObject result) {
        if (result == null) result = Json.obj("status", "failed", "message", "result lost");
        Resp r = call("POST", server, "/api/v1/device/commands/" + id + "/result", result, token);
        if (r.code == 200 || r.code == 404) {
            // 404: the server no longer knows the command, so there's nothing to report to
            JSONObject e = state.done(id);
            if (e != null) state.putDone(id, e.optJSONObject("result"), true);
        }
    }

    private void resendResults(String server, String token) {
        JSONArray pending = state.unsent();
        for (int i = 0; i < pending.length(); i++) {
            JSONObject e = pending.optJSONObject(i);
            if (e != null) sendResult(server, token, e.optLong("id", -1), e.optJSONObject("result"));
        }
    }

    // ---- HTTP ------------------------------------------------------------------

    static final class Resp {
        final int code;          // 0 for network errors
        final JSONObject json;
        final String message;

        Resp(int code, JSONObject json, String message) {
            this.code = code;
            this.json = json;
            this.message = message;
        }
    }

    /** Background thread only. */
    private Resp call(String method, String server, String path, JSONObject body, String token) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(server + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(false); // never send the token elsewhere
            c.setRequestProperty("User-Agent", "Kiosky/" + Updater.currentVersion(context));
            c.setRequestProperty("Accept", "application/json");
            if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                byte[] bytes = body.toString().getBytes("UTF-8");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setFixedLengthStreamingMode(bytes.length);
                OutputStream out = c.getOutputStream();
                out.write(bytes);
                out.close();
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            JSONObject json = Json.parse(in == null ? null : readAll(in));
            String message = "HTTP " + code;
            JSONObject err = json == null ? null : json.optJSONObject("error");
            if (err != null) message = Json.str(err, "message", message);
            return new Resp(code, json, message);
        } catch (IOException e) {
            return new Resp(0, null, e.getMessage() != null ? e.getMessage() : e.toString());
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toString("UTF-8");
    }
}
