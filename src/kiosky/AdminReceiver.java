package kiosky;

import android.app.admin.DeviceAdminReceiver;

/**
 * Device admin component, so Kiosky can be made the tablet's device owner:
 *
 *     adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver
 *
 * As device owner, Kiosky can use full lock task mode (see MainActivity lockdown)
 * and install its updates without Android's confirmation prompt.
 */
public class AdminReceiver extends DeviceAdminReceiver {
}
