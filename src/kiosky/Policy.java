package kiosky;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.Locale;

/**
 * The settings a Shepherd server enforces on a managed tablet (PROTOCOL.md §5).
 * Missing keys fall back to the protocol's defaults, and unknown keys are ignored,
 * so newer servers keep working with this app.
 */
final class Policy {

    final String version;
    final String organisation;

    final String url;
    final String[] allowedHosts;
    final int reloadMinutes;
    final int retrySeconds;
    final int textZoom;
    final String userAgentSuffix;
    final boolean autoplayMedia;
    final boolean thirdPartyCookies;

    final boolean startOnBoot;
    final boolean keepScreenOn;
    final String orientation;
    final boolean allowExit;
    final boolean lockdown;

    final String toolbarMode;        // hidden | limited | full
    final int toolbarTimeoutSeconds;
    final JSONObject pin;            // null: no technician unlock

    final String updateSource;       // server | github | off
    final boolean autoInstall;
    final int checkHours;

    final int checkinSeconds;

    final Options options;           // idle, watchdog, camera/microphone, screen schedule

    final String httpAuthHost;       // "": the start page's host
    final String httpAuthUser;
    final String httpAuthPassword;
    final String[] trustedCertificates; // SHA-256 fingerprints, normalised hex

    Policy(JSONObject p) {
        version = Json.str(p, "version", "");
        organisation = Json.str(p, "organisation", "your organisation");

        url = Json.str(p, "url", "").trim();
        JSONArray hosts = p.optJSONArray("allowed_hosts");
        int n = hosts == null ? 0 : hosts.length();
        allowedHosts = new String[n];
        for (int i = 0; i < n; i++) allowedHosts[i] = hosts.optString(i, "").trim().toLowerCase(Locale.ROOT);
        reloadMinutes = Json.clamp(p.optInt("reload_minutes", 0), 0, 10080);
        retrySeconds = Json.clamp(p.optInt("retry_seconds", 10), 3, 3600);
        textZoom = Json.clamp(p.optInt("text_zoom", 100), 50, 300);
        userAgentSuffix = Json.str(p, "user_agent_suffix", "").trim();
        autoplayMedia = p.optBoolean("autoplay_media", true);
        thirdPartyCookies = p.optBoolean("third_party_cookies", true);

        startOnBoot = p.optBoolean("start_on_boot", true);
        keepScreenOn = p.optBoolean("keep_screen_on", true);
        orientation = Json.str(p, "orientation", "auto");
        allowExit = p.optBoolean("allow_exit", false);
        lockdown = p.optBoolean("lockdown", false);

        JSONObject tb = p.optJSONObject("toolbar");
        String mode = Json.str(tb, "mode", "hidden");
        toolbarMode = ("limited".equals(mode) || "full".equals(mode)) ? mode : "hidden";
        toolbarTimeoutSeconds = Json.clamp(tb == null ? 15 : tb.optInt("timeout_seconds", 15), 5, 600);
        pin = tb == null ? null : tb.optJSONObject("pin");

        JSONObject up = p.optJSONObject("updates");
        String src = Json.str(up, "source", "server");
        updateSource = ("github".equals(src) || "off".equals(src)) ? src : "server";
        autoInstall = up == null || up.optBoolean("auto_install", true);
        checkHours = Json.clamp(up == null ? 12 : up.optInt("check_hours", 12), 1, 168);

        checkinSeconds = Json.clamp(p.optInt("checkin_seconds", 60), 15, 3600);
        options = new Options(p);

        JSONObject auth = p.optJSONObject("http_auth");
        httpAuthHost = Json.str(auth, "host", "").trim().toLowerCase(Locale.ROOT);
        httpAuthUser = Json.str(auth, "username", "");
        httpAuthPassword = Json.str(auth, "password", "");
        JSONArray certs = p.optJSONArray("trusted_certificates");
        int nc = certs == null ? 0 : certs.length();
        trustedCertificates = new String[nc];
        for (int i = 0; i < nc; i++) trustedCertificates[i] = Trust.normalise(certs.optString(i, ""));
    }

    boolean trustsCertificate(String fingerprint) {
        for (String c : trustedCertificates) if (c.length() == 64 && c.equals(fingerprint)) return true;
        return false;
    }

    /** Playlist pages are always allowed, like the start page (§5.2). */
    boolean inPlaylist(String target) {
        String host = host(target);
        if (host == null) return false;
        for (String u : options.playlistUrls) if (host.equals(host(u))) return true;
        return false;
    }

    static Policy parse(String json) {
        JSONObject o = Json.parse(json);
        return o == null ? null : new Policy(o);
    }

    /** Value for Activity.setRequestedOrientation(). */
    int orientationCode() {
        if ("portrait".equals(orientation)) return 1;
        if ("landscape".equals(orientation)) return 0;
        if ("reverse_portrait".equals(orientation)) return 9;
        if ("reverse_landscape".equals(orientation)) return 8;
        return -1; // SCREEN_ORIENTATION_UNSPECIFIED
    }

    /**
     * Whether a navigation may proceed. The start page's host is always allowed;
     * an empty list allows any https host. "*.example.com" matches subdomains only,
     * and "host:port" entries must match the port too.
     */
    boolean allows(String target) {
        String host = host(target);
        if (host == null) return false;
        if (!"https".equalsIgnoreCase(scheme(target))) return false;
        if (host.equals(host(url)) || inPlaylist(target)) return true;
        if (allowedHosts.length == 0) return true;
        return listed(target);
    }

    /**
     * Whether {@code target} is the start page's host or matches an allowed_hosts entry.
     * Unlike {@link #allows}, an empty list allows nothing more: camera and microphone
     * access follows this stricter rule.
     */
    boolean trusted(String target) {
        String host = host(target);
        if (host == null || !"https".equalsIgnoreCase(scheme(target))) return false;
        return host.equals(host(url)) || listed(target);
    }

    private boolean listed(String target) {
        String host = host(target);
        if (host == null) return false;
        int port = port(target);
        for (String entry : allowedHosts) {
            if (entry.length() == 0) continue;
            String h = entry;
            int p = -1;
            int colon = entry.lastIndexOf(':');
            if (colon > 0 && entry.indexOf(']') < colon) {
                try {
                    p = Integer.parseInt(entry.substring(colon + 1));
                    h = entry.substring(0, colon);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            boolean match = h.startsWith("*.")
                    ? host.endsWith(h.substring(1)) && host.length() > h.length() - 1
                    : host.equals(h);
            if (match && (p < 0 || p == port)) return true;
        }
        return false;
    }

    static String host(String url) {
        try {
            String h = new URI(url).getHost();
            return h == null ? null : h.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    private static String scheme(String url) {
        try {
            return new URI(url).getScheme();
        } catch (Exception e) {
            return null;
        }
    }

    private static int port(String url) {
        try {
            int p = new URI(url).getPort();
            return p > 0 ? p : 443;
        } catch (Exception e) {
            return 443;
        }
    }
}
