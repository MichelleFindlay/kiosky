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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        /** A newer release is available. Called on the main thread. */
        void onUpdateAvailable(String version, String apkUrl, long size);
        /** Download or install stopped without updating. Called on the main thread. */
        void onInstallFailed();
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
                listener.onInstallFailed();
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

    String version() {
        return current;
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
                        main.post(new Runnable() {
                            public void run() {
                                listener.onUpdateAvailable(stripV(tag), url, len);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (manual) toast("Couldn't check for updates: " + e.getMessage());
                }
            }
        }).start();
    }

    /** Downloads the APK into an install session and asks Android to install it. */
    void install(final String apkUrl, final long size) {
        if (busy) return;

        // Android 8+ needs "Install unknown apps" turned on for Kiosky (asked once)
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            toast("Allow Kiosky to install apps, then go back and tap Install again");
            Intent i = new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(i);
            listener.onInstallFailed();
            return;
        }

        busy = true;
        toast("Downloading update…");
        new Thread(new Runnable() {
            public void run() {
                PackageInstaller.Session session = null;
                try {
                    PackageInstaller pi = activity.getPackageManager().getPackageInstaller();
                    int id = pi.createSession(new PackageInstaller.SessionParams(1 /* MODE_FULL_INSTALL */));
                    session = pi.openSession(id);

                    HttpURLConnection c = open(apkUrl);
                    if (c.getResponseCode() != 200) throw new IOException("download failed (" + c.getResponseCode() + ")");
                    InputStream in = c.getInputStream();
                    OutputStream out = session.openWrite("kiosky.apk", 0, size);
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    session.fsync(out);
                    out.close();
                    in.close();
                    c.disconnect();

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
                            listener.onInstallFailed();
                        }
                    });
                }
            }
        }).start();
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
