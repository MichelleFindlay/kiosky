package kiosky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Opens Kiosky when the tablet finishes starting up, if "Start on boot" is on.
 * Android 10 and later only allow this once Kiosky may display over other apps.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (!MainActivity.startOnBoot(c)) return;
        try {
            Intent open = new Intent(c, MainActivity.class);
            open.addFlags(0x10000000 /* FLAG_ACTIVITY_NEW_TASK */);
            c.startActivity(open);
        } catch (Exception ignored) {
            // Blocked by this Android version or device maker
        }
    }
}
