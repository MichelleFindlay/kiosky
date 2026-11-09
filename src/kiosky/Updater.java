package kiosky;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Random;

/**
 * Checks GitHub for a newer Kiosky release and installs its APK.
 *
 * A release counts when its tag is a version (v1.2.3) higher than the running app
 * and it has an .apk file attached. Android only accepts the update if that APK is
 * signed with the same key as the installed app.
 */
final class Updater {

    static final String REPO = "MichelleFindlay/kiosky";
    private static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String ACTION_STATUS = "kiosky.UPDATE_STATUS";
    private static final String EXTRA_TOKEN = "token";

    interface Listener {
        /**
         * A newer release is available. {@code publishedAt}: when GitHub published it
         * (0 if unknown); {@code manual}: someone asked for this check. Main thread.
         */
        void onUpdateAvailable(String version, String apkUrl, long size, long publishedAt, boolean manual);
        /** Download or install stopped without updating. Called on the main thread. */
        void onInstallFailed(String reason);
    }

    private final Activity activity;
    private final Listener listener;
    private final Handler main = new Handler();
    private final String token = Long.toHexString(new Random().nextLong());
    private final String current;
    private boolean busy;

    private final BroadcastReceiver status = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // Ignore anything that isn't from our own install session
            if (!token.equals(i.getStringExtra(EXTRA_TOKEN))) return;
            int s = i.getIntExtra("android.content.pm.extra.STATUS", 1 /* STATUS_FAILURE */);
            if (s == -1 /* STATUS_PENDING_USER_ACTION */) {
                Intent confirm = i.getParcelableExtra("android.intent.extra.INTENT");
                if (confirm != null) {
                    confirm.addFlags(0x10000000 /* FLAG_ACTIVITY_NEW_TASK */);
                    activity.startActivity(confirm);
                }
            } else if (s != 0 /* STATUS_SUCCESS */) {
                // Includes the person tapping Cancel on the system's install prompt
                busy = false;
                String msg = i.getStringExtra("android.content.pm.extra.STATUS_MESSAGE");
                toast("Update not installed" + (msg != null ? ": " + msg : ""));
                listener.onInstallFailed(s == 3 ? "cancelled on the device" : (msg != null ? msg : "install status " + s));
            }
            // On success Android replaces the app; UpdateReceiver reopens it
        }
    };

    Updater(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.current = currentVersion(activity);
        activity.registerReceiver(status, new IntentFilter(ACTION_STATUS));
    }

    static String currentVersion(Context c) {
        try {
            String v = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            return v != null ? v : "0";
        } catch (Exception e) {
            return "0";
        }
    }

    static int currentVersionCode(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    String version() {
        return current;
    }

    boolean isBusy() {
        return busy;
    }

    void destroy() {
        try {
            activity.unregisterReceiver(status);
        } catch (Exception ignored) {
            // already gone
        }
    }

    /** Looks for a newer release. With {@code manual}, reports the result either way. */
    void check(final boolean manual) {
        if (busy) return;
        if (manual) toast("Checking for updates…");
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection c = open(LATEST);
                    c.setRequestProperty("Accept", "application/vnd.github+json");
                    int code = c.getResponseCode();
                    if (code == 404) {
                        if (manual) toast("No releases have been published yet");
                        return;
                    }
                    if (code != 200) throw new IOException("GitHub replied " + code);
                    JSONObject rel = new JSONObject(readAll(c.getInputStream()));
                    c.disconnect();

                    final String tag = rel.getString("tag_name");
                    String apk = null;
                    long size = -1;
                    JSONArray assets = rel.optJSONArray("assets");
                    for (int i = 0; assets != null && i < assets.length(); i++) {
                        JSONObject a = assets.optJSONObject(i);
                        if (a != null && a.optString("name").toLowerCase().endsWith(".apk")) {
                            apk = a.optString("browser_download_url");
                            size = a.optLong("size", -1);
                            break;
                        }
                    }

                    if (compare(tag, current) <= 0) {
                        if (manual) toast("Kiosky " + current + " is up to date");
                    } else if (apk == null || apk.length() == 0) {
                        if (manual) toast("Release " + tag + " has no APK attached");
                    } else {
                        final String url = apk;
                        final long len = size;
                        final long published = parseTime(rel.optString("published_at", ""));
                        main.post(new Runnable() {
                            public void run() {
                                listener.onUpdateAvailable(stripV(tag), url, len, published, manual);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (manual) toast("Couldn't check for updates: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Downloads an APK and asks Android to install it. {@code auth} is sent as the
     * Authorization header (Shepherd downloads need it); when {@code sha256} is given,
     * the download must match it or nothing is installed.
     */
    void install(final String apkUrl, final long size, final String auth, final String sha256) {
        if (busy) return;

        // Android 8+ needs "Install unknown apps" turned on for Kiosky (asked once)
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            toast("Allow Kiosky to install apps, then go back and tap Install again");
            Intent i = new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(i);
            listener.onInstallFailed("install permission not granted");
            return;
        }

        busy = true;
        toast("Downloading update…");
        new Thread(new Runnable() {
            public void run() {
                File apk = new File(activity.getCacheDir(), "update.apk");
                PackageInstaller.Session session = null;
                try {
                    // 1. Download to a temporary file, hashing as we go
                    HttpURLConnection c = open(apkUrl);
                    if (auth != null) {
                        c.setInstanceFollowRedirects(false); // keep the token on this server
                        c.setRequestProperty("Authorization", auth);
                    }
                    if (c.getResponseCode() != 200) throw new IOException("download failed (" + c.getResponseCode() + ")");
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    InputStream in = c.getInputStream();
                    OutputStream file = new FileOutputStream(apk);
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        digest.update(buf, 0, n);
                        file.write(buf, 0, n);
                    }
                    file.close();
                    in.close();
                    c.disconnect();
                    if (sha256 != null && !sha256.equalsIgnoreCase(hex(digest.digest()))) {
                        throw new IOException("the download didn't match its checksum");
                    }
                    if (size > 0 && apk.length() != size) {
                        throw new IOException("the download was incomplete");
                    }

                    // 2. Hand it to Android
                    PackageInstaller pi = activity.getPackageManager().getPackageInstaller();
                    int id = pi.createSession(new PackageInstaller.SessionParams(1 /* MODE_FULL_INSTALL */));
                    session = pi.openSession(id);
                    OutputStream out = session.openWrite("kiosky.apk", 0, apk.length());
                    InputStream src = new FileInputStream(apk);
                    while ((n = src.read(buf)) > 0) out.write(buf, 0, n);
                    src.close();
                    session.fsync(out);
                    out.close();

                    Intent cb = new Intent(ACTION_STATUS)
                            .setPackage(activity.getPackageName())
                            .putExtra(EXTRA_TOKEN, token);
                    int flags = 0x08000000; // FLAG_UPDATE_CURRENT
                    if (Build.VERSION.SDK_INT >= 31) flags |= 0x02000000; // FLAG_MUTABLE: installer adds the status
                    PendingIntent pending = PendingIntent.getBroadcast(activity, 0, cb, flags);
                    session.commit(pending.getIntentSender());
                    session.close();
                } catch (final Exception e) {
                    if (session != null) session.abandon();
                    main.post(new Runnable() {
                        public void run() {
                            busy = false;
                            toast("Update failed: " + e.getMessage());
                            listener.onInstallFailed(String.valueOf(e.getMessage()));
                        }
                    });
                } finally {
                    apk.delete();
                }
            }
        }).start();
    }

    /** GitHub's "2026-11-09T13:28:00Z" to milliseconds, or 0. */
    static long parseTime(String iso) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return f.parse(iso).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * How long after a release this tablet installs it automatically, spread evenly
     * over {@code hours} by a hash of the device id, so a bad release reaches only
     * some tablets before it can be pulled.
     */
    static long rolloutDelayMs(Context c, int hours) {
        if (hours <= 0) return 0;
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(Mqtt.deviceId(c).getBytes("UTF-8"));
            long v = ((h[0] & 0xFFL) << 24) | ((h[1] & 0xFFL) << 16) | ((h[2] & 0xFFL) << 8) | (h[3] & 0xFFL);
            return (long) (v / 4294967296.0 * hours * 3600000L);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte x : b) s.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        return s.toString();
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        c.setRequestProperty("User-Agent", "Kiosky/" + current);
        c.setInstanceFollowRedirects(true); // release files redirect to GitHub's download host
        return c;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toString("UTF-8");
    }

    private static String stripV(String v) {
        return v.startsWith("v") || v.startsWith("V") ? v.substring(1) : v;
    }

    /** Compares versions like "v1.2.3" and "1.10.0" by number. */
    static int compare(String a, String b) {
        int[] x = parts(a), y = parts(b);
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) return x[i] < y[i] ? -1 : 1;
        }
        return 0;
    }

    private static int[] parts(String v) {
        int[] p = new int[3];
        String core = stripV(v.trim());
        int dash = core.indexOf('-'); // ignore suffixes such as -beta
        if (dash >= 0) core = core.substring(0, dash);
        String[] s = core.split("\\.");
        for (int i = 0; i < 3 && i < s.length; i++) {
            try {
                p[i] = Integer.parseInt(s[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                p[i] = 0;
            }
        }
        return p;
    }

    private void toast(final String text) {
        main.post(new Runnable() {
            public void run() {
                Toast.makeText(activity, text, 1 /* LENGTH_LONG */).show();
            }
        });
    }
}
