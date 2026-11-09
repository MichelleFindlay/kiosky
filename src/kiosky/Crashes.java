package kiosky;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Remembers an uncaught exception just before the app dies, so the next start can
 * report it to Shepherd as a {@code crash} event and include it in diagnostics.
 */
final class Crashes {

    private static final String PENDING = "pending_crash", LAST = "last_crash"; // in MainActivity.PREFS
    private static boolean installed;

    private Crashes() {}

    static synchronized void install(Context c) {
        if (installed) return;
        installed = true;
        final SharedPreferences prefs = c.getApplicationContext().getSharedPreferences(MainActivity.PREFS, 0);
        final String version = Updater.currentVersion(c);
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String stack = sw.toString();
                    if (stack.length() > 6000) stack = stack.substring(0, 6000);
                    String json = Json.obj("thread", t.getName(), "message", String.valueOf(e),
                            "stack", stack, "app_version", version,
                            "at_ms", System.currentTimeMillis()).toString();
                    prefs.edit().putString(PENDING, json).putString(LAST, json).commit(); // commit: we're about to die
                    UpdateHealth.crashed(prefs, System.currentTimeMillis());
                } catch (Throwable ignored) {
                    // never make a crash worse
                }
                if (previous != null) previous.uncaughtException(t, e);
            }
        });
    }

    /** The crash from the last run, once, or null. */
    static JSONObject takePending(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(MainActivity.PREFS, 0);
        JSONObject j = Json.parse(prefs.getString(PENDING, null));
        if (j != null) prefs.edit().remove(PENDING).apply();
        return j;
    }

    static String last(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, 0).getString(LAST, null);
    }
}
