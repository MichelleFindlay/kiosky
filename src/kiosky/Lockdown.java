package kiosky;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

/**
 * Lockdown support shared by the activity and the accessibility helper.
 *
 * Android only lets the device owner (or an MDM that whitelists Kiosky) truly lock a
 * tablet. Everything else is layered on so that as much as possible is blocked on
 * every Android version:
 *
 *   lock task (owner/MDM) ............ 5.1+  everything
 *   screen pinning ................... 5.1+  Home and Recents, until unpinned
 *   status bar blocker overlay ....... 5.1 – 7.1  pulling down the shade
 *   closing the shade when it opens .. 5.1 – 11 (activity), 12+ (accessibility helper)
 *   returning to Kiosky when left .... ≤9 always, 10+ with "Display over other apps",
 *                                      and any version with the accessibility helper
 *   Kiosky as the Home app ........... 5.1+  the Home button
 */
final class Lockdown {

    static final String HOME_ALIAS = "kiosky.Home";

    /** Until when Kiosky may be left on purpose (Settings, an install prompt, a scanner). */
    static volatile long allowLeaveUntil;

    /** Until when not to close system panels: the "pin this app?" prompt is one. */
    static volatile long quietUntil;

    private Lockdown() {}

    static boolean wanted(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, 0).getBoolean(MainActivity.KEY_LOCKDOWN, false);
    }

    static void allowLeave(long ms) {
        allowLeaveUntil = System.currentTimeMillis() + ms;
    }

    static boolean leaveAllowed() {
        return System.currentTimeMillis() < allowLeaveUntil;
    }

    // ---- Device owner and MDM -------------------------------------------------

    static DevicePolicyManager dpm(Context c) {
        return (DevicePolicyManager) c.getSystemService("device_policy");
    }

    static ComponentName admin(Context c) {
        return new ComponentName(c, AdminReceiver.class);
    }

    static boolean isOwner(Context c) {
        try {
            DevicePolicyManager dpm = dpm(c);
            return dpm != null && dpm.isDeviceOwnerApp(c.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /** True when lock task mode will fully lock (we're owner, or an MDM allowed us). */
    static boolean fullLockPossible(Context c) {
        if (isOwner(c)) return true;
        if (Build.VERSION.SDK_INT < 23) return false;
        try {
            return dpm(c).isLockTaskPermitted(c.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /** "owner", "mdm" or "partial", for the toolbar and status reports. */
    static String mode(Context c) {
        if (isOwner(c)) return "owner";
        return fullLockPossible(c) ? "mdm" : "partial";
    }

    // ---- Kiosky as the Home app -------------------------------------------------

    /**
     * Enables or disables the Home activity alias. As device owner, Kiosky also makes
     * itself the default Home app; otherwise the person picks it once.
     */
    static void setHome(Context c, boolean on) {
        try {
            c.getPackageManager().setComponentEnabledSetting(new ComponentName(c.getPackageName(), HOME_ALIAS),
                    on ? 1 /* ENABLED */ : 2 /* DISABLED */, 1 /* DONT_KILL_APP */);
        } catch (Exception ignored) {
            // leave as it was
        }
        if (!isOwner(c)) return;
        try {
            DevicePolicyManager dpm = dpm(c);
            if (on) {
                IntentFilter f = new IntentFilter("android.intent.action.MAIN");
                f.addCategory("android.intent.category.HOME");
                f.addCategory("android.intent.category.DEFAULT");
                dpm.addPersistentPreferredActivity(admin(c), f, new ComponentName(c.getPackageName(), HOME_ALIAS));
            } else {
                dpm.clearPackagePersistentPreferredActivities(admin(c), c.getPackageName());
            }
        } catch (Exception ignored) {
            // not permitted
        }
    }

    static boolean isDefaultHome(Context c) {
        try {
            Intent home = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
            ResolveInfo ri = c.getPackageManager().resolveActivity(home, 0x10000 /* MATCH_DEFAULT_ONLY */);
            return ri != null && ri.activityInfo != null && c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- Returning to Kiosky ------------------------------------------------------

    static void bringToFront(Context c) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService("power");
            if (pm != null && !pm.isInteractive()) return; // screen off: don't fight the lock screen
            Intent i = new Intent(c, MainActivity.class);
            i.addFlags(0x10000000 /* NEW_TASK */ | 0x00020000 /* REORDER_TO_FRONT */);
            c.startActivity(i);
        } catch (Exception ignored) {
            // blocked by this Android version
        }
    }

    /** Whether Android lets Kiosky reopen itself from the background on this tablet. */
    static boolean canReturn(Context c) {
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(c) || KioskAccessibility.running();
    }

    // ---- Closing the notification shade ------------------------------------------

    /** Closes the notification shade by every means this Android version allows. */
    static void collapseShade(Context c) {
        if (System.currentTimeMillis() < quietUntil) return;
        if (Build.VERSION.SDK_INT >= 31 && KioskAccessibility.dismissShade()) return;
        try {
            Object sb = c.getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager").getMethod("collapsePanels").invoke(sb);
        } catch (Throwable ignored) {
            // hidden API not available
        }
        if (Build.VERSION.SDK_INT < 31) {
            try {
                c.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
            } catch (Throwable ignored) {
                // refused
            }
        }
    }

    // ---- Status bar blocker (Android 5.1 – 7.1) ------------------------------------

    /**
     * An invisible strip over the status bar that swallows the swipe that would pull
     * down the shade. Android 8 stopped apps drawing over the status bar, so this only
     * runs up to 7.1, and from 6.0 it needs "Display over other apps".
     */
    static final class StatusBarBlocker {
        private View view;

        void show(Activity a) {
            if (view != null || Build.VERSION.SDK_INT >= 26) return;
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(a)) return;
            try {
                Context app = a.getApplicationContext();
                int height = 0;
                int id = a.getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (id > 0) height = a.getResources().getDimensionPixelSize(id);
                if (height <= 0) height = (int) (25 * a.getResources().getDisplayMetrics().density);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, height,
                        2010 /* TYPE_SYSTEM_ERROR: above the status bar */,
                        0x8 | 0x20 | 0x100 /* NOT_FOCUSABLE | NOT_TOUCH_MODAL | LAYOUT_IN_SCREEN */,
                        -3 /* PixelFormat.TRANSLUCENT */);
                lp.gravity = 48; // TOP
                View v = new View(app);
                ((WindowManager) app.getSystemService("window")).addView(v, lp);
                view = v;
            } catch (Exception ignored) {
                // not allowed on this tablet
            }
        }

        void hide(Activity a) {
            if (view == null) return;
            try {
                ((WindowManager) a.getApplicationContext().getSystemService("window")).removeView(view);
            } catch (Exception ignored) {
                // already gone
            }
            view = null;
        }

        boolean active() {
            return view != null;
        }
    }
}
