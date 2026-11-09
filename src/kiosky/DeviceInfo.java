package kiosky;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Facts about the tablet, for enrollment (§3.2) and check-in status (§4.1). */
final class DeviceInfo {

    private static final long MB = 1024L * 1024L;
    private static final long APP_START = SystemClock.elapsedRealtime();
    private static final Pattern CHROME = Pattern.compile("Chrome/([0-9.]+)");

    private DeviceInfo() {}

    static JSONObject device(Context c) {
        return Json.obj(
                "android_id", Settings.Secure.getString(c.getContentResolver(), "android_id"),
                "manufacturer", Build.MANUFACTURER,
                "model", Build.MODEL,
                "android_version", Build.VERSION.RELEASE,
                "sdk_int", Build.VERSION.SDK_INT,
                "app_version", Updater.currentVersion(c),
                "app_version_code", Updater.currentVersionCode(c));
    }

    static boolean overlayPermission(Context c) {
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(c);
    }

    static boolean installPermission(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    /** Main thread. {@code url}, {@code title} and {@code pageError} come from the WebView. */
    static JSONObject status(Activity a, String url, String title, String pageError, String userAgent) {
        JSONObject s = Json.obj(
                "current_url", url,
                "page_title", title,
                "page_error", pageError,
                "start_on_boot_permission", overlayPermission(a),
                "install_permission", installPermission(a),
                "uptime_seconds", SystemClock.elapsedRealtime() / 1000,
                "app_uptime_seconds", (SystemClock.elapsedRealtime() - APP_START) / 1000);

        try {
            PowerManager pm = (PowerManager) a.getSystemService("power");
            if (pm != null) Json.put(s, "screen_on", pm.isInteractive());
        } catch (Exception ignored) {
            // leave it out
        }

        Intent battery = Battery.read(a);
        if (battery != null) {
            int level = Battery.level(battery);
            if (level >= 0) Json.put(s, "battery_level", level);
            Json.put(s, "charging", Battery.charging(battery));
            float t = Battery.temperature(battery);
            if (!Float.isNaN(t)) Json.put(s, "battery_temperature", Math.round(t * 10) / 10.0);
            Json.put(s, "battery_health", Battery.health(battery));
        }

        try {
            StatFs fs = new StatFs(a.getFilesDir().getPath());
            Json.put(s, "storage_free_mb", fs.getAvailableBytes() / MB);
            Json.put(s, "storage_total_mb", fs.getTotalBytes() / MB);
        } catch (Exception ignored) {
            // leave it out
        }
        try {
            ActivityManager am = (ActivityManager) a.getSystemService("activity");
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            Json.put(s, "memory_free_mb", mi.availMem / MB);
            Json.put(s, "memory_total_mb", mi.totalMem / MB);
            Json.put(s, "memory_low", mi.lowMemory);
            Runtime rt = Runtime.getRuntime();
            Json.put(s, "app_memory_mb", (rt.totalMemory() - rt.freeMemory()) / MB);
        } catch (Exception ignored) {
            // leave it out
        }

        if (userAgent != null) {
            Matcher m = CHROME.matcher(userAgent);
            if (m.find()) Json.put(s, "webview_version", m.group(1));
        }

        int o = a.getResources().getConfiguration().orientation;
        Json.put(s, "orientation", o == 2 ? "landscape" : (o == 1 ? "portrait" : "unknown"));
        DisplayMetrics dm = a.getResources().getDisplayMetrics();
        Json.put(s, "screen", dm.widthPixels + "x" + dm.heightPixels);

        String ip = localIp();
        if (ip != null) Json.put(s, "local_ip", ip);

        try {
            WifiManager wm = (WifiManager) a.getApplicationContext().getSystemService("wifi");
            WifiInfo info = wm == null ? null : wm.getConnectionInfo();
            if (info != null && info.getRssi() > -127) Json.put(s, "wifi_rssi", info.getRssi());
        } catch (Exception ignored) {
            // no Wi-Fi or no permission
        }
        return s;
    }

    private static String localIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs != null && ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {
            // leave it out
        }
        return null;
    }
}
