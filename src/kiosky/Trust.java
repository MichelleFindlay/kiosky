package kiosky;

import android.content.Context;
import android.net.http.SslCertificate;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * Certificates and sign-ins a kiosk page may need:
 *
 * - Self-signed certificates (common on local Home Assistant servers) are trusted
 *   only by their SHA-256 fingerprint: from the policy's {@code trusted_certificates}
 *   on a managed tablet, or confirmed once on the tablet when unmanaged.
 * - HTTP sign-in (basic auth) details: from the policy's {@code http_auth.*}, or typed
 *   once on an unmanaged tablet and remembered for that host.
 */
final class Trust {

    private static final String CERTS = "trusted_certs";  // "host|fingerprint," list, unmanaged
    private static final String AUTH = "http_auth";       // {host: {username, password}}, unmanaged

    private Trust() {}

    /** Lower-case hex SHA-256 of the certificate's DER encoding, or null. */
    static String fingerprint(SslCertificate cert) {
        if (cert == null) return null;
        try {
            byte[] der = null;
            if (Build.VERSION.SDK_INT >= 29 && cert.getX509Certificate() != null) {
                der = cert.getX509Certificate().getEncoded();
            } else {
                Bundle b = SslCertificate.saveState(cert);
                if (b != null) der = b.getByteArray("x509-certificate");
            }
            if (der == null) return null;
            byte[] h = MessageDigest.getInstance("SHA-256").digest(der);
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(String.format(Locale.ROOT, "%02x", x & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** "ab12…" or "AB:12:…" to plain lower-case hex. */
    static String normalise(String fp) {
        return fp == null ? "" : fp.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
    }

    /** AB:12:… for showing people. */
    static String pretty(String fp) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < fp.length(); i += 2) {
            if (sb.length() > 0) sb.append(':');
            sb.append(fp.substring(i, i + 2).toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }

    static boolean localCert(Context c, String host, String fp) {
        String list = c.getSharedPreferences(MainActivity.PREFS, 0).getString(CERTS, ",");
        return list.contains("," + host + "|" + fp + ",");
    }

    static void addLocalCert(Context c, String host, String fp) {
        String list = c.getSharedPreferences(MainActivity.PREFS, 0).getString(CERTS, ",");
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().putString(CERTS, list + host + "|" + fp + ",").apply();
    }

    static JSONObject localAuth(Context c, String host) {
        JSONObject all = Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(AUTH, null));
        return all == null ? null : all.optJSONObject(host);
    }

    static void saveLocalAuth(Context c, String host, String user, String password) {
        JSONObject all = Json.parse(c.getSharedPreferences(MainActivity.PREFS, 0).getString(AUTH, null));
        if (all == null) all = new JSONObject();
        Json.put(all, host, Json.obj("username", user, "password", password));
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().putString(AUTH, all.toString()).apply();
    }

    /** Forgets every certificate and sign-in confirmed on this tablet. */
    static void forgetLocal(Context c) {
        c.getSharedPreferences(MainActivity.PREFS, 0).edit().remove(CERTS).remove(AUTH).apply();
    }
}
