import com.android.apksig.ApkVerifier;

import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Shows which certificate Android uses to check an APK on each version, so a key
 * rotation can be confirmed before release:
 *   java -cp .tools/signer.jar tools/CheckSigning.java kiosky.apk
 */
public class CheckSigning {
    public static void main(String[] args) throws Exception {
        File apk = new File(args.length > 0 ? args[0] : "kiosky.apk");
        for (int sdk : new int[] {22, 26, 28, 30, 32, 33, 35}) {
            ApkVerifier.Result r = new ApkVerifier.Builder(apk)
                    .setMinCheckedPlatformVersion(sdk).setMaxCheckedPlatformVersion(sdk).build().verify();
            List<X509Certificate> certs = r.getSignerCertificates();
            String who = certs.isEmpty() ? "none" : describe(certs.get(0));
            String lineage = r.getSigningCertificateLineage() == null ? "no lineage"
                    : r.getSigningCertificateLineage().size() + "-certificate lineage";
            System.out.printf("Android SDK %d: %s, %s%s%n", sdk, r.isVerified() ? "verified" : "NOT VERIFIED", who,
                    r.isVerifiedUsingV3Scheme() || r.isVerifiedUsingV31Scheme() ? " (v3, " + lineage + ")" : " (v1/v2)");
        }
        // Android 8.1 and earlier can't read v3 at all: they check v2 (7.0+) or v1
        ApkVerifier.Result all = new ApkVerifier.Builder(apk).build().verify();
        for (ApkVerifier.Result.V2SchemeSignerInfo s : all.getV2SchemeSigners()) {
            System.out.println("v2 (Android 7.0–8.1): " + describe(s.getCertificate()));
        }
        for (ApkVerifier.Result.V1SchemeSignerInfo s : all.getV1SchemeSigners()) {
            System.out.println("v1 (Android 5.1–6.0): " + describe(s.getCertificate()));
        }
    }

    private static String describe(X509Certificate c) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(c.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(String.format("%02x", h[i]));
        return c.getSubjectX500Principal().getName() + " [" + sb + "…]";
    }
}
