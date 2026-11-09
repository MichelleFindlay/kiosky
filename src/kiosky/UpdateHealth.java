package kiosky;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Watches a newly installed version for its first {@link #TRIAL_MS}. Two crashes in
 * that time mark it unhealthy: Kiosky reports {@code update_unhealthy} and starts in
 * safe mode, with its optional extras (MQTT, the motion sensors, Wi-Fi changes,
 * restoring the last page) off, so it keeps checking in for the next update.
 * Surviving the trial reports {@code update_healthy}, which a server can use to widen
 * a staged rollout.
 *
 * Android never lets an app install an older version of itself, so the way out of an
 * unhealthy version is always a newer one (an old build released with a higher
 * versionCode will do).
 */
final class UpdateHealth {

    static final long TRIAL_MS = 15 * 60 * 1000L;
    private static final int CRASHES = 2;
    private static final String KEY = "update_health";   // in MainActivity.PREFS
    private static final String LAST_CODE = "last_version_code";

    private UpdateHealth() {}

    /**
     * At every start, first thing. A version code different from the last start's
     * begins a trial for it; the very first install doesn't need one.
     */
    static synchronized void started(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(MainActivity.PREFS, 0);
        int code = Updater.currentVersionCode(c);
        int last = prefs.getInt(LAST_CODE, 0);
        if (code == last) return;
        SharedPreferences.Editor ed = prefs.edit().putInt(LAST_CODE, code)
                .putString("last_version", Updater.currentVersion(c));
        if (last == 0 && updatedFromOlder(c)) last = -1; // updated from a version before 1.8.0, which didn't record it
        if (last != 0) {
            ed.putString(KEY, Json.obj(
                    "version", Updater.currentVersion(c), "version_code", code,
                    "previous_version", prefs.getString("last_version", ""), "previous_version_code", Math.max(last, 0),
                    "since", System.currentTimeMillis(), "crashes", new JSONArray(),
                    "state", "trial", "reported", false).toString());
        } else {
            ed.remove(KEY);
        }
        ed.commit();
    }

    /** Installed as an update rather than fresh (Android's install and update times differ). */
    private static boolean updatedFromOlder(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.lastUpdateTime > pi.firstInstallTime + 1000;
        } catch (Exception e) {
            return false;
        }
    }

    /** From the crash handler, just before the process dies (so: commit). */
    static void crashed(SharedPreferences prefs, long now) {
        JSONObject h = Json.parse(prefs.getString(KEY, null));
        if (h == null || !"trial".equals(h.optString("state")) || now - h.optLong("since") >= TRIAL_MS) return;
        JSONArray crashes = h.optJSONArray("crashes");
        if (crashes == null) crashes = new JSONArray();
        crashes.put(now);
        Json.put(h, "crashes", crashes);
        if (crashes.length() >= CRASHES) Json.put(h, "state", "unhealthy");
        prefs.edit().putString(KEY, h.toString()).commit();
    }

    /** This version crashed repeatedly right after it was installed. */
    static boolean safeMode(Context c) {
        JSONObject h = Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(KEY, null));
        return h != null && "unhealthy".equals(h.optString("state"))
                && h.optInt("version_code") == Updater.currentVersionCode(c);
    }

    /**
     * Main thread, regularly. Returns an event to report once, as {type, data}:
     * {@code update_unhealthy} as soon as it's known, {@code update_healthy} when a
     * trial ends without enough crashes. Otherwise null.
     */
    static synchronized JSONObject check(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(MainActivity.PREFS, 0);
        JSONObject h = Json.parse(prefs.getString(KEY, null));
        if (h == null || h.optBoolean("reported", false)) return null;
        String state = h.optString("state");
        String type;
        if ("unhealthy".equals(state)) {
            type = "update_unhealthy";
        } else if ("trial".equals(state) && System.currentTimeMillis() - h.optLong("since") >= TRIAL_MS) {
            Json.put(h, "state", "healthy");
            type = "update_healthy";
        } else {
            return null;
        }
        Json.put(h, "reported", true);
        prefs.edit().putString(KEY, h.toString()).apply();
        JSONArray crashes = h.optJSONArray("crashes");
        JSONObject data = Json.obj("version", h.optString("version"), "version_code", h.optInt("version_code"),
                "previous_version", h.optString("previous_version"),
                "previous_version_code", h.optInt("previous_version_code"),
                "crashes", crashes == null ? 0 : crashes.length());
        JSONObject crash = Json.parse(Crashes.last(c));
        if ("update_unhealthy".equals(type) && crash != null) Json.put(data, "last_crash", Json.str(crash, "message", ""));
        return Json.obj("type", type, "data", data);
    }

    /** For status reports: trial, healthy or unhealthy (null before any update). */
    static String state(Context c) {
        JSONObject h = Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(KEY, null));
        if (h == null || h.optInt("version_code") != Updater.currentVersionCode(c)) return null;
        return h.optString("state", null);
    }
}
