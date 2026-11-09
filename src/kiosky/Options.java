package kiosky;

import android.content.Context;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * Page and screen behaviour that works the same managed or not: from the Shepherd
 * policy on a managed tablet, from the local Settings dialog otherwise. Both use the
 * same JSON shape, so a retired tablet keeps what its policy had.
 *
 * <pre>
 * "idle_home_minutes": 0,          // back to the start page after this long untouched (0: never)
 * "watchdog_minutes": 2,           // reload a page that stops answering, then rebuild (0: never)
 * "media": { "camera": true, "microphone": true },
 * "screen": {
 *   "off_at": "", "on_at": "",     // screen off between these local times (HH:MM)
 *   "wake_on_touch": true, "wake_on_motion": false, "wake_minutes": 5,
 *   "dim_at": "", "dim_until": "", "dim_brightness": 15,
 *   "idle_dim_minutes": 0          // dim after this long untouched (0: never)
 * },
 * "battery": { "protect": false }, // the tablet's own charging limit, where there is one (Battery)
 * "playlist": { "urls": [], "seconds": 60, "pause_on_touch_seconds": 120 },
 * "screensaver": { "minutes": 0, "url": "" },   // clock (or a page) after this long untouched
 * "audio": { "volume": -1, "quiet_at": "", "quiet_until": "", "quiet_volume": 0 },
 * "file_uploads": true,
 * "reboot_at": ""                  // daily restart (a full reboot when Kiosky is device owner)
 * </pre>
 */
final class Options {

    static final String PREF = "options"; // in MainActivity.PREFS

    static final int NORMAL = 0, DIM = 1, OFF = 2;

    final int idleHomeMinutes;
    final int watchdogMinutes;
    final boolean camera;
    final boolean microphone;

    final int offAt, onAt;          // minutes after midnight; -1: no schedule
    final boolean wakeOnTouch;
    final boolean wakeOnMotion;
    final int wakeMinutes;
    final int dimAt, dimUntil;      // minutes after midnight; -1: no dimming
    final int dimBrightness;        // percent
    final int idleDimMinutes;
    final boolean batteryProtect;

    final String[] playlistUrls;
    final int playlistSeconds;
    final int playlistPauseSeconds;
    final int screensaverMinutes;
    final String screensaverUrl;
    final int volume;               // percent; -1: leave the tablet's volume alone
    final int quietAt, quietUntil;  // minutes after midnight; -1: no quiet hours
    final int quietVolume;          // percent during quiet hours
    final boolean fileUploads;
    final int rebootAt;             // minutes after midnight; -1: never

    Options(JSONObject o) {
        if (o == null) o = new JSONObject();
        idleHomeMinutes = Json.clamp(o.optInt("idle_home_minutes", 0), 0, 1440);
        watchdogMinutes = Json.clamp(o.optInt("watchdog_minutes", 2), 0, 60);
        JSONObject media = o.optJSONObject("media");
        camera = media == null || media.optBoolean("camera", true);
        microphone = media == null || media.optBoolean("microphone", true);

        JSONObject s = o.optJSONObject("screen");
        int off = time(Json.str(s, "off_at", ""));
        int on = time(Json.str(s, "on_at", ""));
        boolean schedule = off >= 0 && on >= 0 && off != on;
        offAt = schedule ? off : -1;
        onAt = schedule ? on : -1;
        wakeOnTouch = s == null || s.optBoolean("wake_on_touch", true);
        wakeOnMotion = s != null && s.optBoolean("wake_on_motion", false);
        wakeMinutes = Json.clamp(s == null ? 5 : s.optInt("wake_minutes", 5), 1, 240);
        int dimFrom = time(Json.str(s, "dim_at", ""));
        int dimTo = time(Json.str(s, "dim_until", ""));
        boolean dims = dimFrom >= 0 && dimTo >= 0 && dimFrom != dimTo;
        dimAt = dims ? dimFrom : -1;
        dimUntil = dims ? dimTo : -1;
        dimBrightness = Json.clamp(s == null ? 15 : s.optInt("dim_brightness", 15), 1, 100);
        idleDimMinutes = Json.clamp(s == null ? 0 : s.optInt("idle_dim_minutes", 0), 0, 1440);
        JSONObject battery = o.optJSONObject("battery");
        batteryProtect = battery != null && battery.optBoolean("protect", false);

        JSONObject pl = o.optJSONObject("playlist");
        org.json.JSONArray urls = pl == null ? null : pl.optJSONArray("urls");
        java.util.List<String> list = new java.util.ArrayList<String>();
        for (int i = 0; urls != null && i < urls.length() && list.size() < 20; i++) {
            String u = urls.optString(i, "").trim();
            if (u.startsWith("https://")) list.add(u);
        }
        playlistUrls = list.toArray(new String[0]);
        playlistSeconds = Json.clamp(pl == null ? 60 : pl.optInt("seconds", 60), 10, 86400);
        playlistPauseSeconds = Json.clamp(pl == null ? 120 : pl.optInt("pause_on_touch_seconds", 120), 0, 3600);

        JSONObject ss = o.optJSONObject("screensaver");
        screensaverMinutes = Json.clamp(ss == null ? 0 : ss.optInt("minutes", 0), 0, 1440);
        String su = Json.str(ss, "url", "").trim();
        screensaverUrl = su.startsWith("https://") ? su : "";

        JSONObject au = o.optJSONObject("audio");
        volume = au == null ? -1 : Json.clamp(au.optInt("volume", -1), -1, 100);
        int qa = time(Json.str(au, "quiet_at", ""));
        int qu = time(Json.str(au, "quiet_until", ""));
        boolean quiet = qa >= 0 && qu >= 0 && qa != qu;
        quietAt = quiet ? qa : -1;
        quietUntil = quiet ? qu : -1;
        quietVolume = Json.clamp(au == null ? 0 : au.optInt("quiet_volume", 0), 0, 100);

        fileUploads = o.optBoolean("file_uploads", true);
        rebootAt = time(Json.str(o, "reboot_at", ""));
    }

    /** The volume wanted right now, in percent, or -1 to leave it alone. */
    int volumeNow(Calendar now) {
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (quietAt >= 0 && within(m, quietAt, quietUntil)) return quietVolume;
        return volume;
    }

    /** The unmanaged tablet's own settings. */
    static Options local(Context c) {
        return new Options(Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(PREF, null)));
    }

    static void saveLocal(Context c, JSONObject o) {
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().putString(PREF, o.toString()).apply();
    }

    /** Back to JSON, for saving and for keeping a retired tablet's settings. */
    JSONObject toJson() {
        return Json.obj(
                "idle_home_minutes", idleHomeMinutes,
                "watchdog_minutes", watchdogMinutes,
                "media", Json.obj("camera", camera, "microphone", microphone),
                "screen", Json.obj(
                        "off_at", format(offAt), "on_at", format(onAt),
                        "wake_on_touch", wakeOnTouch, "wake_on_motion", wakeOnMotion,
                        "wake_minutes", wakeMinutes,
                        "dim_at", format(dimAt), "dim_until", format(dimUntil),
                        "dim_brightness", dimBrightness,
                        "idle_dim_minutes", idleDimMinutes),
                "battery", Json.obj("protect", batteryProtect),
                "playlist", Json.obj("urls", array(playlistUrls),
                        "seconds", playlistSeconds, "pause_on_touch_seconds", playlistPauseSeconds),
                "screensaver", Json.obj("minutes", screensaverMinutes, "url", screensaverUrl),
                "audio", Json.obj("volume", volume, "quiet_at", format(quietAt), "quiet_until", format(quietUntil),
                        "quiet_volume", quietVolume),
                "file_uploads", fileUploads,
                "reboot_at", format(rebootAt));
    }

    private static org.json.JSONArray array(String[] values) {
        org.json.JSONArray a = new org.json.JSONArray();
        for (String v : values) a.put(v);
        return a;
    }

    boolean hasSchedule() {
        return offAt >= 0;
    }

    /** Touch or motion can bring the screen back during the off hours. */
    boolean wakeable() {
        return wakeOnTouch || wakeOnMotion;
    }

    /** What the schedule asks for right now: NORMAL, DIM or OFF. */
    int scheduled(Calendar now) {
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (offAt >= 0 && within(m, offAt, onAt)) return OFF;
        if (dimAt >= 0 && within(m, dimAt, dimUntil)) return DIM;
        return NORMAL;
    }

    /** Wall-clock time of the next {@code on_at}, for the alarm that wakes a sleeping tablet. */
    long nextOnAt(Calendar now) {
        Calendar c = (Calendar) now.clone();
        c.set(Calendar.HOUR_OF_DAY, onAt / 60);
        c.set(Calendar.MINUTE, onAt % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (!c.after(now)) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    /** from ≤ m < to, wrapping past midnight when {@code to} is earlier. */
    static boolean within(int m, int from, int to) {
        return from < to ? (m >= from && m < to) : (m >= from || m < to);
    }

    /** "HH:MM" (24-hour) to minutes after midnight, or -1. */
    static int time(String s) {
        if (s == null) return -1;
        s = s.trim().replace('.', ':');
        int colon = s.indexOf(':');
        try {
            int h, m;
            if (colon < 0) {
                if (s.length() == 0 || s.length() > 4) return -1;
                int v = Integer.parseInt(s); // "7", "0700", "2230"
                h = s.length() <= 2 ? v : v / 100;
                m = s.length() <= 2 ? 0 : v % 100;
            } else {
                h = Integer.parseInt(s.substring(0, colon).trim());
                m = Integer.parseInt(s.substring(colon + 1).trim());
            }
            if (h == 24 && m == 0) h = 0;
            return h >= 0 && h < 24 && m >= 0 && m < 60 ? h * 60 + m : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String format(int minutes) {
        if (minutes < 0) return "";
        int h = minutes / 60, m = minutes % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }
}
