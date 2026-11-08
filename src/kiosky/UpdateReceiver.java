package kiosky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Reopens Kiosky after it has updated itself, since Android closes the app to
 * replace it. Android 10 and later may block this; then tap the Kiosky icon.
 */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            Intent open = new Intent(c, MainActivity.class);
            open.addFlags(0x10000000 /* FLAG_ACTIVITY_NEW_TASK */);
            c.startActivity(open);
        } catch (Exception ignored) {
            // Not allowed to start from the background on this Android version
        }
    }
}
