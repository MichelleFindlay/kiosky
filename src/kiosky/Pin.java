package kiosky;

import android.util.Base64;

import org.json.JSONObject;

import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks a technician PIN against the server's PBKDF2-SHA256 hash (PROTOCOL.md §5.3),
 * and hashes the local settings PIN the same way.
 *
 * PBKDF2 is computed here on HmacSHA256 rather than with SecretKeyFactory, because
 * "PBKDF2WithHmacSHA256" only exists from Android 8, and Kiosky supports Android 5.1.
 * It's slow on purpose (about 120,000 rounds), so call it off the main thread.
 */
final class Pin {

    private Pin() {}

    static boolean verify(String pin, JSONObject spec) {
        if (pin == null || pin.length() == 0 || spec == null) return false;
        if (!"pbkdf2-sha256".equals(spec.optString("algo"))) return false;
        int iterations = spec.optInt("iterations", 0);
        if (iterations < 1 || iterations > 10000000) return false;
        try {
            byte[] salt = Base64.decode(spec.optString("salt"), Base64.DEFAULT);
            byte[] expected = Base64.decode(spec.optString("hash"), Base64.DEFAULT);
            if (expected.length == 0) return false;
            byte[] actual = pbkdf2(pin.getBytes("UTF-8"), salt, iterations, expected.length);
            return constantTimeEquals(actual, expected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hashes a new PIN in the same format as the server's (for the local settings PIN
     * on unmanaged tablets). Slow, like {@link #verify}.
     */
    static JSONObject create(String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            int iterations = 100000;
            byte[] hash = pbkdf2(pin.getBytes("UTF-8"), salt, iterations, 32);
            return Json.obj("algo", "pbkdf2-sha256", "iterations", iterations,
                    "salt", Base64.encodeToString(salt, Base64.NO_WRAP),
                    "hash", Base64.encodeToString(hash, Base64.NO_WRAP));
        } catch (Exception e) {
            return null;
        }
    }

    /** PBKDF2 with HMAC-SHA256 (RFC 8018). */
    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(password, "HmacSHA256"));
        int hLen = mac.getMacLength();
        byte[] out = new byte[length];
        int blocks = (length + hLen - 1) / hLen;
        for (int b = 1; b <= blocks; b++) {
            mac.update(salt);
            mac.update(new byte[] {(byte) (b >>> 24), (byte) (b >>> 16), (byte) (b >>> 8), (byte) b});
            byte[] u = mac.doFinal();
            byte[] t = u.clone();
            for (int i = 1; i < iterations; i++) {
                u = mac.doFinal(u);
                for (int j = 0; j < t.length; j++) t[j] ^= u[j];
            }
            int off = (b - 1) * hLen;
            System.arraycopy(t, 0, out, off, Math.min(hLen, length - off));
        }
        return out;
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}
