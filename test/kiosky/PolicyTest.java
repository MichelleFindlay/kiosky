package kiosky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class PolicyTest {

    private static Policy policy(String json) {
        return new Policy(Json.parse(json));
    }

    @Test
    public void defaultsFollowTheProtocol() {
        Policy p = policy("{}");
        assertEquals("", p.url);
        assertEquals(10, p.retrySeconds);
        assertEquals(100, p.textZoom);
        assertTrue(p.startOnBoot);
        assertTrue(p.keepScreenOn);
        assertFalse(p.allowExit);
        assertFalse(p.lockdown);
        assertEquals("hidden", p.toolbarMode);
        assertEquals(15, p.toolbarTimeoutSeconds);
        assertNull(p.pin);
        assertEquals("server", p.updateSource);
        assertTrue(p.autoInstall);
        assertEquals(12, p.checkHours);
        assertEquals(0, p.rolloutHours);
        assertEquals(60, p.checkinSeconds);
        assertNull(p.wifiNetworks);
        assertEquals("your organisation", p.organisation);
    }

    @Test
    public void valuesAreClampedAndUnknownEnumsFallBack() {
        Policy p = policy("{\"retry_seconds\": 1, \"text_zoom\": 900, \"checkin_seconds\": 5,"
                + "\"toolbar\": {\"mode\": \"sideways\", \"timeout_seconds\": 9999},"
                + "\"updates\": {\"source\": \"ftp\", \"check_hours\": 0, \"rollout_hours\": 999}}");
        assertEquals(3, p.retrySeconds);
        assertEquals(300, p.textZoom);
        assertEquals(15, p.checkinSeconds);
        assertEquals("hidden", p.toolbarMode);
        assertEquals(600, p.toolbarTimeoutSeconds);
        assertEquals("server", p.updateSource);
        assertEquals(1, p.checkHours);
        assertEquals(336, p.rolloutHours);
    }

    @Test
    public void orientationCodes() {
        assertEquals(-1, policy("{}").orientationCode());
        assertEquals(1, policy("{\"orientation\": \"portrait\"}").orientationCode());
        assertEquals(0, policy("{\"orientation\": \"landscape\"}").orientationCode());
        assertEquals(9, policy("{\"orientation\": \"reverse_portrait\"}").orientationCode());
        assertEquals(8, policy("{\"orientation\": \"reverse_landscape\"}").orientationCode());
        assertEquals(-1, policy("{\"orientation\": \"diagonal\"}").orientationCode());
    }

    @Test
    public void emptyAllowListAllowsAnyHttpsSite() {
        Policy p = policy("{\"url\": \"https://ha.example.com/lovelace\"}");
        assertTrue(p.allows("https://anything.example.org/"));
        assertFalse(p.allows("http://ha.example.com/")); // never plain http
        assertFalse(p.allows("not a url"));
        assertFalse(p.allows("javascript:alert(1)"));
    }

    @Test
    public void allowListMatching() {
        Policy p = policy("{\"url\": \"https://ha.example.com/\", \"allowed_hosts\": [\"*.cdn.example\", \"cams.example:8443\", \"Docs.Example\"]}");
        assertTrue("the start page's host", p.allows("https://ha.example.com/other"));
        assertTrue("wildcard subdomain", p.allows("https://img.cdn.example/a.png"));
        assertFalse("wildcards don't match the bare domain", p.allows("https://cdn.example/"));
        assertFalse("nor a lookalike", p.allows("https://evilcdn.example/"));
        assertTrue("host:port", p.allows("https://cams.example:8443/live"));
        assertFalse("host:port needs that port", p.allows("https://cams.example/live"));
        assertTrue("case-insensitive", p.allows("https://docs.example/x"));
        assertFalse(p.allows("https://other.example/"));
    }

    @Test
    public void trustedIsStricterThanAllowed() {
        Policy open = policy("{\"url\": \"https://ha.example.com/\"}");
        assertTrue(open.trusted("https://ha.example.com/x"));
        assertFalse("an empty list trusts nothing more", open.trusted("https://elsewhere.example/"));
        Policy listed = policy("{\"url\": \"https://ha.example.com/\", \"allowed_hosts\": [\"cams.example\"]}");
        assertTrue(listed.trusted("https://cams.example/"));
        assertFalse(listed.trusted("http://cams.example/"));
    }

    @Test
    public void playlistHostsAreAlwaysAllowed() {
        Policy p = policy("{\"url\": \"https://ha.example.com/\", \"allowed_hosts\": [\"x.example\"],"
                + "\"playlist\": {\"urls\": [\"https://calendar.example/week\", \"http://insecure.example/\"]}}");
        assertTrue(p.allows("https://calendar.example/day"));
        assertEquals("plain http playlist entries are dropped", 1, p.options.playlistUrls.length);
        assertFalse(p.allows("https://insecure.example/"));
    }

    @Test
    public void certificateFingerprintsAreNormalised() {
        String hex = "ab12" + new String(new char[60]).replace('\0', '0');
        Policy p = policy("{\"trusted_certificates\": [\"AB:12:" + hex.substring(4).replaceAll("(..)", "$1:").replaceAll(":$", "") + "\"]}");
        assertTrue(p.trustsCertificate(hex));
        assertFalse(p.trustsCertificate(hex.replace('0', '1')));
        assertFalse("short entries never match", policy("{\"trusted_certificates\": [\"ab\"]}").trustsCertificate("ab"));
    }

    @Test
    public void wifiNetworksAreValidatedAndNormalised() {
        Policy p = policy("{\"wifi\": {\"networks\": ["
                + "{\"ssid\": \"Office\", \"password\": \"office-pass\"},"
                + "{\"ssid\": \"Short\", \"password\": \"abc\"},"
                + "{\"ssid\": \"Guest\"},"
                + "{\"ssid\": \"\"},"
                + "{\"ssid\": \"Back\", \"password\": \"password9\", \"security\": \"WPA3\", \"hidden\": true}]}}");
        JSONArray n = p.wifiNetworks;
        assertEquals(3, n.length());
        JSONObject office = n.optJSONObject(0);
        assertEquals("wpa2", office.optString("security"));
        assertEquals("open", n.optJSONObject(1).optString("security"));
        assertEquals("", n.optJSONObject(1).optString("password"));
        assertEquals("wpa3", n.optJSONObject(2).optString("security"));
        assertTrue(n.optJSONObject(2).optBoolean("hidden", false));
    }

    @Test
    public void emptyWifiListMeansLeaveWifiAlone() {
        assertNull(policy("{\"wifi\": {\"networks\": []}}").wifiNetworks);
        assertNull("only invalid entries", policy("{\"wifi\": {\"networks\": [{\"ssid\": \"\"}]}}").wifiNetworks);
        assertNull(policy("{\"wifi\": {}}").wifiNetworks);
    }

    @Test
    public void optionsComeFromThePolicy() {
        Policy p = policy("{\"idle_home_minutes\": 5, \"screen\": {\"off_at\": \"22:00\", \"on_at\": \"07:00\", \"pixel_shift\": true},"
                + "\"session\": {\"clear_on_idle\": true}, \"touch\": {\"long_press\": false},"
                + "\"mqtt\": {\"host\": \"ha.local\", \"topic\": \"/wall/#hall/\"}, \"input\": {\"nfc\": true}}");
        Options o = p.options;
        assertEquals(5, o.idleHomeMinutes);
        assertTrue(o.hasSchedule());
        assertTrue(o.pixelShift);
        assertTrue(o.clearSessionOnIdle);
        assertFalse(o.longPress);
        assertTrue(o.textSelection);
        assertFalse(o.zoom);
        assertTrue(o.mqttEnabled());
        assertEquals("wall/hall", o.mqttTopic);
        assertEquals(1883, o.mqttPort);
        assertTrue(o.nfc);
        assertTrue(o.barcode);
        assertTrue(o.offlineLastPage);
    }

    @Test
    public void optionsSurviveARoundTrip() {
        Options o = policy("{\"screen\": {\"dim_at\": \"20:00\", \"dim_until\": \"06:30\"}, \"audio\": {\"volume\": 40},"
                + "\"mqtt\": {\"host\": \"10.0.0.2\", \"tls\": true, \"password\": \"p w\"}, \"touch\": {\"zoom\": true}}").options;
        Options back = new Options(o.toJson());
        assertEquals(o.mqttKey(), back.mqttKey());
        assertEquals(8883, back.mqttPort);
        assertEquals(o.dimAt, back.dimAt);
        assertEquals(40, back.volume);
        assertTrue(back.zoom);
    }

    @Test
    public void times() {
        assertEquals(22 * 60 + 30, Options.time("22:30"));
        assertEquals(7 * 60, Options.time("7"));
        assertEquals(7 * 60 + 5, Options.time("0705"));
        assertEquals(0, Options.time("24:00"));
        assertEquals(-1, Options.time("25:00"));
        assertEquals(-1, Options.time("noon"));
        assertEquals("07:05", Options.format(7 * 60 + 5));
        assertTrue(Options.within(23 * 60, 22 * 60, 7 * 60)); // wraps past midnight
        assertFalse(Options.within(12 * 60, 22 * 60, 7 * 60));
    }
}
