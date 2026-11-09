package kiosky;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

/**
 * Turns the screen back on at the schedule's {@code on_at} when it went fully off
 * (no wake on touch or motion, so the tablet was allowed to sleep). The activity's
 * own timers stop while the tablet sleeps, so this uses an alarm.
 */
public class ScheduleReceiver extends BroadcastReceiver {

    static final String EXTRA_WAKE = "kiosky.wake";

    @Override
    public void onReceive(Context c, Intent i) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService("power");
            // SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE
            PowerManager.WakeLock wl = pm.newWakeLock(0x0000000a | 0x10000000 | 0x20000000, "kiosky:schedule");
            wl.acquire(15000);
        } catch (Exception ignored) {
            // can't wake the screen; the activity still restores it when next opened
        }
        try {
            Intent open = new Intent(c, MainActivity.class);
            open.addFlags(0x10000000 /* NEW_TASK */ | 0x00020000 /* REORDER_TO_FRONT */);
            open.putExtra(EXTRA_WAKE, true);
            c.startActivity(open);
        } catch (Exception ignored) {
            // blocked by this Android version
        }
    }

    static void set(Context c, long at) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService("alarm");
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(0 /* RTC_WAKEUP */, at, pending(c));
            else am.setExact(0 /* RTC_WAKEUP */, at, pending(c));
        } catch (Exception ignored) {
            // no alarms on this tablet
        }
    }

    static void cancel(Context c) {
        try {
            ((AlarmManager) c.getSystemService("alarm")).cancel(pending(c));
        } catch (Exception ignored) {
            // nothing set
        }
    }

    private static PendingIntent pending(Context c) {
        int flags = 0x08000000 /* UPDATE_CURRENT */ | (Build.VERSION.SDK_INT >= 23 ? 0x04000000 /* IMMUTABLE */ : 0);
        return PendingIntent.getBroadcast(c, 7, new Intent(c, ScheduleReceiver.class), flags);
    }
}
