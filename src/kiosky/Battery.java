package kiosky;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Battery care for tablets that sit on charge all the time, which wears batteries
 * out and can make them swell: temperature and health reporting, a "hot" event, how
 * long it has sat full, and (experimentally) the charging limit some tablets have.
 *
 * Android has no public way for an app to limit charging. Samsung's "Protect battery"
 * (stop at about 85%) is the global setting {@code protect_battery}, which Kiosky can
 * change once it has been granted WRITE_SECURE_SETTINGS over adb:
 * {@code adb shell pm grant uk.lp0.ha android.permission.WRITE_SECURE_SETTINGS}.
 */
final class Battery {

    private static final String SAMSUNG_KEY = "protect_battery";
    private static final String PREF_SET_BY_US = "battery_protect_set"; // in MainActivity.PREFS
    private static final float HOT_C = 45f, COOL_C = 42f;

    Battery() {}

    /** The latest battery broadcast (sticky, so this doesn't wait). */
    static Intent read(Context c) {
        try {
            return c.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        } catch (Exception e) {
            return null;
        }
    }

    /** Degrees Celsius, or NaN when unknown. */
    static float temperature(Intent b) {
        int t = b == null ? Integer.MIN_VALUE : b.getIntExtra("temperature", Integer.MIN_VALUE);
        return t == Integer.MIN_VALUE ? Float.NaN : t / 10f; // tenths of a degree
    }

    static int level(Intent b) {
        if (b == null) return -1;
        int level = b.getIntExtra("level", -1);
        int scale = b.getIntExtra("scale", -1);
        return level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
    }

    static boolean charging(Intent b) {
        return b != null && b.getIntExtra("plugged", 0) != 0;
    }

    /** BatteryManager.BATTERY_HEALTH_* as words. */
    static String health(Intent b) {
        switch (b == null ? 1 : b.getIntExtra("health", 1)) {
            case 2: return "good";
            case 3: return "overheat";
            case 4: return "dead";
            case 5: return "over_voltage";
            case 6: return "failure";
            case 7: return "cold";
            default: return "unknown";
        }
    }

    // ---- Watching it while Kiosky runs -----------------------------------------

    private boolean hot;
    private long fullSince; // elapsedRealtime when it reached 100% on charge; 0: not full

    /**
     * Call about once a minute. Returns "hot" when the battery first reaches 45 °C,
     * "cool" when it drops back under 42 °C, otherwise null.
     */
    String check(Intent b) {
        if (b == null) return null;
        long now = SystemClock.elapsedRealtime();
        if (charging(b) && level(b) >= 100) {
            if (fullSince == 0) fullSince = now;
        } else {
            fullSince = 0;
        }
        float t = temperature(b);
        if (Float.isNaN(t)) return null;
        if (!hot && t >= HOT_C) {
            hot = true;
            return "hot";
        }
        if (hot && t < COOL_C) {
            hot = false;
            return "cool";
        }
        return null;
    }

    /** Whole hours it has sat at 100% on charge, while Kiosky has been running. */
    int fullHours() {
        return fullSince == 0 ? 0 : (int) ((SystemClock.elapsedRealtime() - fullSince) / 3600000L);
    }

    // ---- Charging limit ------------------------------------------------------------

    /**
     * Turns the tablet's own charging limit on or off. Only switches it off again if
     * Kiosky was the one that switched it on, so a limit set by hand is left alone.
     * Returns "on", "off", "no_permission" or "unsupported".
     */
    static String applyProtect(Context c, boolean on) {
        boolean setByUs = c.getSharedPreferences(MainActivity.PREFS, 0).getBoolean(PREF_SET_BY_US, false);
        if (!on && !setByUs) return "off";
        if (!"samsung".equalsIgnoreCase(Build.MANUFACTURER)) return on ? "unsupported" : "off";
        ContentResolver r = c.getContentResolver();
        try {
            int current = Settings.Global.getInt(r, SAMSUNG_KEY, -1);
            if (current == -1) return on ? "unsupported" : "off"; // not this tablet's setting
            int want = on ? 1 : 0;
            if (current != want) {
                if (Build.VERSION.SDK_INT >= 23
                        && c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                    return on ? "no_permission" : "off";
                }
                Settings.Global.putInt(r, SAMSUNG_KEY, want);
                c.getSharedPreferences(MainActivity.PREFS, 0).edit().putBoolean(PREF_SET_BY_US, on).apply();
            }
            return on ? "on" : "off";
        } catch (Exception e) {
            return on ? "no_permission" : "off"; // SecurityException without the grant
        }
    }
}
