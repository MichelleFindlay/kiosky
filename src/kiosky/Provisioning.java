package kiosky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import org.json.JSONObject;

/**
 * QR provisioning: a factory-reset tablet scans a code during setup (tap the welcome
 * screen six times), connects to Wi-Fi, downloads Kiosky and makes it the device
 * owner, with no computer or adb. Kiosky Shepherd generates the code; its admin
 * extras carry the server and enrollment code, so Kiosky enrolls by itself.
 *
 * <pre>
 * "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": { "server": "https://…", "token": "ABCD…" }
 * </pre>
 */
final class Provisioning {

    private static final String EXTRAS = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE";
    private static final String PREF = "pending_enroll"; // in MainActivity.PREFS

    private Provisioning() {}

    /** Keeps the server and code from a provisioning intent until the activity enrolls. */
    static void store(Context c, Intent i) {
        try {
            PersistableBundle extras = i == null ? null : (PersistableBundle) i.getParcelableExtra(EXTRAS);
            if (extras == null) return;
            String server = extras.getString("server");
            String token = extras.getString("token");
            if (server == null || token == null || !server.startsWith("https://")) return;
            c.getSharedPreferences(MainActivity.PREFS, 0).edit()
                    .putString(PREF, Json.obj("server", server, "token", token).toString())
                    .putBoolean(MainActivity.KEY_BOOT, true) // a provisioned kiosk should come back after power cuts
                    .commit();
        } catch (Exception ignored) {
            // nothing usable
        }
    }

    /** {server, token} waiting to be used, or null. */
    static JSONObject pending(Context c) {
        return Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(PREF, null));
    }

    static void clear(Context c) {
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().remove(PREF).apply();
    }

    static void openKiosky(Context c) {
        try {
            Intent open = new Intent(c, MainActivity.class);
            open.addFlags(0x10000000 /* NEW_TASK */);
            c.startActivity(open);
        } catch (Exception ignored) {
            // the setup wizard opens the launcher, which is Kiosky once lockdown is on
        }
    }

    /**
     * Android 12+ asks the admin app two things during QR provisioning. Both answer
     * at once: "fully managed device", and "policies are in place".
     *   android.app.action.GET_PROVISIONING_MODE
     *   android.app.action.ADMIN_POLICY_COMPLIANCE
     */
    public static class Step extends Activity {
        @Override
        protected void onCreate(Bundle state) {
            super.onCreate(state);
            Intent i = getIntent();
            store(this, i);
            if (i != null && "android.app.action.GET_PROVISIONING_MODE".equals(i.getAction())) {
                Intent result = new Intent();
                result.putExtra("android.app.extra.PROVISIONING_MODE", 1 /* FULLY_MANAGED_DEVICE */);
                setResult(-1 /* RESULT_OK */, result);
            } else {
                setResult(-1);
                openKiosky(this);
            }
            finish();
        }
    }
}
