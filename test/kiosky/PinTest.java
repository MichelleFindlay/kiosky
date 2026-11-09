package kiosky;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PinTest {

    /** RFC 7914 §11: PBKDF2-HMAC-SHA256, P = "passwd", S = "salt", c = 1, dkLen = 64. */
    @Test
    public void matchesRfc7914TestVector() throws Exception {
        byte[] dk = Pin.pbkdf2("passwd".getBytes("UTF-8"), "salt".getBytes("UTF-8"), 1, 64);
        assertEquals("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
                + "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783", ScanInput.hex(dk));
    }

    /** Against the JDK's own PBKDF2WithHmacSHA256 (the server uses PHP's hash_pbkdf2, the same algorithm). */
    @Test
    public void matchesTheJdkImplementation() throws Exception {
        Random r = new Random(42);
        for (int i = 0; i < 5; i++) {
            String pin = String.valueOf(1000 + r.nextInt(900000));
            byte[] salt = new byte[16];
            r.nextBytes(salt);
            int iterations = 1 + r.nextInt(3000);
            int length = 16 + r.nextInt(48); // more than one HMAC block sometimes
            byte[] ours = Pin.pbkdf2(pin.getBytes("UTF-8"), salt, iterations, length);
            byte[] jdk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(pin.toCharArray(), salt, iterations, length * 8)).getEncoded();
            assertArrayEquals("pin " + pin + ", " + iterations + " rounds, " + length + " bytes", jdk, ours);
        }
    }

    @Test
    public void createdPinVerifies() {
        JSONObject spec = Pin.create("4711");
        assertNotNull(spec);
        assertEquals("pbkdf2-sha256", spec.optString("algo"));
        assertTrue(Pin.verify("4711", spec));
        assertFalse(Pin.verify("4712", spec));
        assertFalse(Pin.verify("", spec));
        assertFalse(Pin.verify(null, spec));
    }

    @Test
    public void refusesSpecsItCantTrust() throws Exception {
        JSONObject good = Pin.create("123456");
        JSONObject algo = new JSONObject(good.toString()).put("algo", "md5");
        assertFalse(Pin.verify("123456", algo));
        JSONObject zero = new JSONObject(good.toString()).put("iterations", 0);
        assertFalse(Pin.verify("123456", zero));
        JSONObject huge = new JSONObject(good.toString()).put("iterations", 20000000);
        assertFalse(Pin.verify("123456", huge)); // would hang the tablet
        JSONObject noHash = new JSONObject(good.toString()).put("hash", "");
        assertFalse(Pin.verify("123456", noHash));
        assertFalse(Pin.verify("123456", null));
    }

    @Test
    public void verifiesAServerStyleHash() throws Exception {
        // What Shepherd's Policy::hashPin sends: base64 salt and 32-byte hash
        byte[] salt = "0123456789abcdef".getBytes("UTF-8");
        byte[] hash = Pin.pbkdf2("2468".getBytes("UTF-8"), salt, 10000, 32);
        JSONObject spec = Json.obj("algo", "pbkdf2-sha256", "iterations", 10000,
                "salt", java.util.Base64.getEncoder().encodeToString(salt),
                "hash", java.util.Base64.getEncoder().encodeToString(hash));
        assertTrue(Pin.verify("2468", spec));
        assertFalse(Pin.verify("2469", spec));
    }
}
