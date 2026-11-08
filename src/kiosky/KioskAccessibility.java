package kiosky;

import android.accessibilityservice.AccessibilityService;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

/**
 * Optional lockdown helper, turned on in Settings › Accessibility.
 *
 * While Lockdown is on it watches which app is in front and nothing else (it doesn't
 * read screen content). If another app, the launcher or Recents comes to front, it
 * brings Kiosky back; on Android 12+ it also closes the notification shade, which
 * Android no longer lets ordinary apps do. Accessibility services may reopen their
 * app from the background on every Android version.
 */
public class KioskAccessibility extends AccessibilityService {

    private static volatile KioskAccessibility instance;

    static boolean running() {
        return instance != null;
    }

    /** Android 12+: closes the notification shade. Returns false if the helper is off. */
    static boolean dismissShade() {
        KioskAccessibility s = instance;
        return s != null && s.performGlobalAction(15 /* GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE */);
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != 32 /* TYPE_WINDOW_STATE_CHANGED */) return;
        if (!Lockdown.wanted(this) || Lockdown.leaveAllowed() || Lockdown.fullLockPossible(this)) return;
        CharSequence p = e.getPackageName();
        if (p == null) return;
        String pkg = p.toString();
        if (pkg.equals(getPackageName()) || pkg.equals("android") || pkg.equals(keyboardPackage())) return;

        if (pkg.equals("com.android.systemui")) {
            // The shade, Recents or a system panel opened over Kiosky
            Lockdown.collapseShade(this);
        } else {
            // Another app, the launcher or Recents (in the launcher on Android 9+)
            Lockdown.bringToFront(this);
        }
    }

    @Override
    public void onInterrupt() {
        // nothing to stop
    }

    private String keyboardPackage() {
        String ime = Settings.Secure.getString(getContentResolver(), "default_input_method");
        if (ime == null) return "";
        int slash = ime.indexOf('/');
        return slash > 0 ? ime.substring(0, slash) : ime;
    }
}
