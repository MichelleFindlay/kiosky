package kiosky;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Device admin component, so Kiosky can be made the tablet's device owner, either
 * with adb on a set-up tablet:
 *
 *     adb shell dpm set-device-owner uk.lp0.ha/kiosky.AdminReceiver
 *
 * or with a provisioning QR code scanned during a factory-reset tablet's setup
 * (see {@link Provisioning}). As device owner, Kiosky can use full lock task mode
 * (see MainActivity lockdown) and install its updates without Android's prompt.
 */
public class AdminReceiver extends DeviceAdminReceiver {

    /** QR provisioning finished (Android 11 and earlier; 12+ also use ProvisioningActivity). */
    @Override
    public void onProfileProvisioningComplete(Context c, Intent i) {
        Provisioning.store(c, i);
        Provisioning.openKiosky(c);
    }
}
