package kiosky;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * What a managed tablet stores about its Shepherd server (PROTOCOL.md §2), in its
 * own private preferences file. Managed mode is on whenever a device token is set.
 */
final class ManagedState {

    static final String FILE = "managed";
    private static final int MAX_EVENTS = 200;   // kept locally; 50 are sent per check-in
    private static final int MAX_DONE = 100;

    private final SharedPreferences prefs;

    ManagedState(Context c) {
        prefs = c.getSharedPreferences(FILE, 0);
    }

    static boolean isManaged(Context c) {
        return c.getSharedPreferences(FILE, 0).getString("device_token", null) != null;
    }

    /** The cached policy, for code that runs without the activity (BootReceiver). */
    static Policy cachedPolicy(Context c) {
        return Policy.parse(c.getSharedPreferences(FILE, 0).getString("policy", null));
    }

    synchronized boolean isManaged() { return token() != null; }
    synchronized String server() { return prefs.getString("server", null); }
    synchronized String token() { return prefs.getString("device_token", null); }
    synchronized String deviceId() { return prefs.getString("device_id", ""); }
    synchronized String name() { return prefs.getString("name", ""); }
    synchronized String policyJson() { return prefs.getString("policy", null); }
    synchronized String appliedVersion() { return prefs.getString("applied_version", null); }

    synchronized void enrolled(String server, String token, String deviceId, String name, String policy) {
        prefs.edit()
                .putString("server", server)
                .putString("device_token", token)
                .putString("device_id", deviceId)
                .putString("name", name)
                .putString("policy", policy)
                .putString("events", "[]")
                .putString("done_commands", "[]")
                .apply();
    }

    /** Saved before applying, so a crash mid-apply re-applies it on restart. */
    synchronized void savePolicy(String json) {
        prefs.edit().putString("policy", json).apply();
    }

    synchronized void setApplied(String version) {
        prefs.edit().putString("applied_version", version).apply();
    }

    synchronized void setName(String name) {
        if (name != null && !name.equals(name())) prefs.edit().putString("name", name).apply();
    }

    /** Leaves managed mode: forgets the token, policy and queues. */
    synchronized void clear() {
        prefs.edit().clear().commit();
    }

    // ---- Events, queued until a check-in delivers them ----------------------

    synchronized void addEvent(String type, JSONObject data) {
        if (!isManaged()) return;
        JSONArray old = Json.parseArray(prefs.getString("events", null));
        JSONArray out = new JSONArray();
        int start = Math.max(0, old.length() - (MAX_EVENTS - 1)); // drop the oldest when full
        for (int i = start; i < old.length(); i++) out.put(old.opt(i));
        out.put(Json.obj("type", type, "at", now(), "data", data));
        prefs.edit().putString("events", out.toString()).apply();
    }

    /** The oldest {@code max} events, without removing them. */
    synchronized JSONArray peekEvents(int max) {
        JSONArray all = Json.parseArray(prefs.getString("events", null));
        JSONArray out = new JSONArray();
        for (int i = 0; i < all.length() && i < max; i++) out.put(all.opt(i));
        return out;
    }

    /** Removes the first {@code n} events once the server has them. */
    synchronized void dropEvents(int n) {
        if (n <= 0) return;
        JSONArray all = Json.parseArray(prefs.getString("events", null));
        JSONArray out = new JSONArray();
        for (int i = n; i < all.length(); i++) out.put(all.opt(i));
        prefs.edit().putString("events", out.toString()).apply();
    }

    // ---- Finished commands, for de-duplication and result re-sending ---------

    /** The stored entry {id, result, sent} for a command, or null if it hasn't run. */
    synchronized JSONObject done(long id) {
        JSONArray all = Json.parseArray(prefs.getString("done_commands", null));
        for (int i = 0; i < all.length(); i++) {
            JSONObject e = all.optJSONObject(i);
            if (e != null && e.optLong("id", -1) == id) return e;
        }
        return null;
    }

    synchronized void putDone(long id, JSONObject result, boolean sent) {
        JSONArray all = Json.parseArray(prefs.getString("done_commands", null));
        JSONArray out = new JSONArray();
        for (int i = 0; i < all.length(); i++) {
            JSONObject e = all.optJSONObject(i);
            if (e != null && e.optLong("id", -1) != id) out.put(e);
        }
        out.put(Json.obj("id", id, "result", result, "sent", sent));
        JSONArray trimmed = new JSONArray();
        for (int i = Math.max(0, out.length() - MAX_DONE); i < out.length(); i++) trimmed.put(out.opt(i));
        prefs.edit().putString("done_commands", trimmed.toString()).apply();
    }

    /** Commands whose result hasn't reached the server yet. */
    synchronized JSONArray unsent() {
        JSONArray all = Json.parseArray(prefs.getString("done_commands", null));
        JSONArray out = new JSONArray();
        for (int i = 0; i < all.length(); i++) {
            JSONObject e = all.optJSONObject(i);
            if (e != null && !e.optBoolean("sent", false)) out.put(e);
        }
        return out;
    }

    static String now() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
