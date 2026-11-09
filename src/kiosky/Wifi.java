package kiosky;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Wi-Fi networks from the policy's {@code wifi.networks}, saved on the tablet so a
 * router change or a new password doesn't need a site visit. Android 10 and later
 * only allow this for the device owner; older versions allow any app.
 *
 * Changes are made so that a mistake can't strand the tablet:
 * <ul>
 * <li>New networks are added before old ones go. Networks Kiosky saved that the
 *     policy no longer lists are removed only once Wi-Fi is working again, and never
 *     the one in use.</li>
 * <li>A changed password or security type for a saved network is put back as it was
 *     if Wi-Fi isn't connected {@link #TRIAL_MS} later, and that policy isn't tried
 *     again until it changes.</li>
 * </ul>
 * Kiosky only ever touches networks it saved itself.
 */
final class Wifi {

    static final long TRIAL_MS = 10 * 60 * 1000L;
    private static final long SETTLE_MS = 2 * 60 * 1000L;

    // In MainActivity.PREFS
    private static final String OURS = "wifi_ours";       // {ssid: {id, spec}} networks Kiosky saved
    private static final String BACKUP = "wifi_backup";   // OURS as it was before the change on trial
    private static final String WANTED = "wifi_wanted";   // the policy list on trial or applied
    private static final String APPLIED = "wifi_applied"; // that list, as text, to spot changes
    private static final String FAILED = "wifi_failed";   // a list that was reverted
    private static final String SINCE = "wifi_since";     // when the change on trial was made; 0: none
    private static final String STATUS = "wifi_status";

    private Wifi() {}

    /** Whether Android lets Kiosky save networks. */
    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 29 || Lockdown.isOwner(c);
    }

    /** off, pending, applied, reverted, failed or not_device_owner. */
    static String status(Context c) {
        return c.getSharedPreferences(MainActivity.PREFS, 0).getString(STATUS, "off");
    }

    /**
     * Saves the policy's networks if they changed. {@code networks} is null when the
     * policy has no {@code wifi} key: Kiosky then leaves Wi-Fi alone. Returns an event
     * ({type, data}) or null.
     */
    static synchronized JSONObject apply(Context c, JSONArray networks) {
        SharedPreferences prefs = c.getSharedPreferences(MainActivity.PREFS, 0);
        if (networks == null) {
            if (prefs.getString(APPLIED, null) == null) prefs.edit().putString(STATUS, "off").apply();
            return null;
        }
        String text = networks.toString();
        if (text.equals(prefs.getString(APPLIED, null)) || text.equals(prefs.getString(FAILED, null))) return null;
        if (!allowed(c)) {
            prefs.edit().putString(STATUS, "not_device_owner").apply();
            return null;
        }
        WifiManager wm = manager(c);
        if (wm == null) return null;

        JSONObject ours = Json.parse(prefs.getString(OURS, null));
        if (ours == null) ours = new JSONObject();
        JSONObject backup = Json.parse(ours.toString());
        JSONArray failed = new JSONArray();
        JSONArray wanted = new JSONArray();
        boolean changedExisting = false;
        for (int i = 0; i < networks.length(); i++) {
            JSONObject spec = networks.optJSONObject(i);
            if (spec == null) continue;
            String ssid = Json.str(spec, "ssid", "");
            wanted.put(ssid);
            JSONObject have = ours.optJSONObject(ssid);
            if (have != null && spec.toString().equals(String.valueOf(have.optJSONObject("spec")))) continue;
            int id = save(wm, spec, have == null ? -1 : have.optInt("id", -1), networks.length() - i);
            if (id < 0) {
                failed.put(ssid);
                continue;
            }
            if (have != null) changedExisting = true;
            Json.put(ours, ssid, Json.obj("id", id, "spec", spec));
            try {
                wm.enableNetwork(id, false);
            } catch (Exception ignored) {
                // Android picks among saved networks by itself anyway
            }
        }
        if (Build.VERSION.SDK_INT < 26) {
            try {
                wm.saveConfiguration();
            } catch (Exception ignored) {
                // newer versions save by themselves
            }
        }
        SharedPreferences.Editor ed = prefs.edit()
                .putString(OURS, ours.toString())
                .putString(WANTED, wanted.toString())
                .putString(APPLIED, text)
                .remove(FAILED)
                .putLong(SINCE, System.currentTimeMillis())
                .putString(STATUS, failed.length() > 0 ? "failed" : "pending");
        // Only a changed network can lose a working connection: keep its old settings to go back to
        if (changedExisting || prefs.getString(BACKUP, null) == null) ed.putString(BACKUP, backup.toString());
        ed.apply();
        if (!connected(c)) {
            try {
                wm.reconnect();
            } catch (Exception ignored) {
                // it'll connect by itself
            }
        }
        return Json.obj("type", failed.length() > 0 ? "wifi_failed" : "wifi_changed",
                "data", Json.obj("networks", wanted, "failed", failed));
    }

    /**
     * Once a minute. Settles a change on trial: confirmed once Wi-Fi is connected,
     * put back if it still isn't after {@link #TRIAL_MS}. Returns an event or null.
     */
    static synchronized JSONObject check(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(MainActivity.PREFS, 0);
        long since = prefs.getLong(SINCE, 0);
        if (since == 0) return null;
        long age = System.currentTimeMillis() - since;
        if (age < 0) age = TRIAL_MS; // the clock jumped back: decide now
        WifiManager wm = manager(c);
        if (wm == null) return null;
        JSONObject ours = Json.parse(prefs.getString(OURS, null));
        if (ours == null) ours = new JSONObject();
        JSONArray wanted = Json.parseArray(prefs.getString(WANTED, null));

        if (age >= SETTLE_MS && connected(c)) {
            // Working: forget networks the policy dropped, except the one in use
            String current = currentSsid(c);
            JSONArray removed = new JSONArray();
            for (Iterator<String> it = keys(ours); it.hasNext(); ) {
                String ssid = it.next();
                if (contains(wanted, ssid) || current == null || ssid.equals(current)) continue;
                JSONObject have = ours.optJSONObject(ssid);
                try {
                    if (have != null) wm.removeNetwork(have.optInt("id", -1));
                } catch (Exception ignored) {
                    // gone already
                }
                ours.remove(ssid);
                removed.put(ssid);
            }
            boolean failed = "failed".equals(prefs.getString(STATUS, ""));
            prefs.edit().putString(OURS, ours.toString()).remove(BACKUP).putLong(SINCE, 0)
                    .putString(STATUS, failed ? "failed" : "applied").apply();
            return Json.obj("type", "wifi_applied", "data",
                    Json.obj("networks", wanted, "removed", removed, "ssid", current));
        }
        if (age < TRIAL_MS) return null;

        // Still no Wi-Fi: put back any network whose settings changed
        JSONObject backup = Json.parse(prefs.getString(BACKUP, null));
        JSONArray restored = new JSONArray();
        if (backup != null) {
            for (Iterator<String> it = keys(backup); it.hasNext(); ) {
                String ssid = it.next();
                JSONObject old = backup.optJSONObject(ssid);
                JSONObject now = ours.optJSONObject(ssid);
                if (old == null || now == null) continue;
                JSONObject spec = old.optJSONObject("spec");
                if (spec == null || spec.toString().equals(String.valueOf(now.optJSONObject("spec")))) continue;
                int id = save(wm, spec, now.optInt("id", -1), 1);
                if (id >= 0) {
                    Json.put(ours, ssid, Json.obj("id", id, "spec", spec));
                    restored.put(ssid);
                }
            }
        }
        if (restored.length() == 0) {
            // Nothing changed that could have cost the connection (new networks only, or out of range)
            prefs.edit().remove(BACKUP).putLong(SINCE, 0).putString(STATUS, "applied").apply();
            return null;
        }
        prefs.edit().putString(OURS, ours.toString()).remove(BACKUP).putLong(SINCE, 0)
                .putString(FAILED, prefs.getString(APPLIED, "")).remove(APPLIED)
                .putString(STATUS, "reverted").apply();
        try {
            wm.reconnect();
        } catch (Exception ignored) {
            // it'll connect by itself
        }
        return Json.obj("type", "wifi_reverted", "data", Json.obj("restored", restored, "networks", wanted));
    }

    /** Adds or updates one network. Returns its id, or -1. */
    private static int save(WifiManager wm, JSONObject spec, int existingId, int priority) {
        WifiConfiguration w = new WifiConfiguration();
        String ssid = Json.str(spec, "ssid", "");
        String password = Json.str(spec, "password", "");
        String security = Json.str(spec, "security", "wpa2");
        w.SSID = "\"" + ssid.replace("\"", "\\\"") + "\"";
        w.hiddenSSID = spec.optBoolean("hidden", false);
        w.priority = priority; // only used before Android 10
        if ("open".equals(security) || password.length() == 0) {
            w.allowedKeyManagement.set(0 /* NONE */);
        } else {
            boolean hex = password.length() == 64 && password.matches("[0-9a-fA-F]+");
            w.preSharedKey = hex ? password : "\"" + password + "\"";
            if ("wpa3".equals(security) && Build.VERSION.SDK_INT >= 30) {
                w.setSecurityParams(4 /* SECURITY_TYPE_SAE */);
            } else if ("wpa3".equals(security) && Build.VERSION.SDK_INT == 29) {
                w.allowedKeyManagement.set(8 /* SAE */);
                w.requirePmf = true;
            } else {
                w.allowedKeyManagement.set(1 /* WPA_PSK */);
            }
        }
        try {
            if (existingId >= 0) {
                w.networkId = existingId;
                int id = wm.updateNetwork(w);
                if (id >= 0) return id;
                wm.removeNetwork(existingId); // couldn't update in place: replace it
            }
            return wm.addNetwork(w);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Connected over Wi-Fi right now. */
    static boolean connected(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService("connectivity");
            NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected() && ni.getType() == 1 /* TYPE_WIFI */;
        } catch (Exception e) {
            return false;
        }
    }

    /** The network in use, or null when Android won't say (it needs the location permission). */
    static String currentSsid(Context c) {
        try {
            WifiManager wm = manager(c);
            WifiInfo info = wm == null ? null : wm.getConnectionInfo();
            String s = info == null ? null : info.getSSID();
            if (s == null || s.length() == 0 || s.contains("<unknown ssid>") || "0x".equals(s)) return null;
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private static WifiManager manager(Context c) {
        try {
            return (WifiManager) c.getApplicationContext().getSystemService("wifi");
        } catch (Exception e) {
            return null;
        }
    }

    /** A copy of the keys, since callers remove entries while going through them. */
    private static Iterator<String> keys(JSONObject o) {
        return toList(o.keys()).iterator();
    }

    private static java.util.List<String> toList(Iterator<String> it) {
        java.util.List<String> l = new java.util.ArrayList<String>();
        while (it.hasNext()) l.add(it.next());
        return l;
    }

    private static boolean contains(JSONArray a, String s) {
        for (int i = 0; i < a.length(); i++) if (s.equals(a.optString(i))) return true;
        return false;
    }
}
