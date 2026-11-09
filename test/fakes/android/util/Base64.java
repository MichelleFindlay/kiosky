package android.util;

/** Test-time stand-in for Android's Base64, on java.util.Base64 (the stub in stubs/ does nothing). */
public class Base64 {
    public static final int DEFAULT = 0, NO_WRAP = 2;

    public static byte[] decode(String s, int flags) {
        return java.util.Base64.getMimeDecoder().decode(s);
    }

    public static String encodeToString(byte[] b, int flags) {
        return java.util.Base64.getEncoder().encodeToString(b);
    }
}
