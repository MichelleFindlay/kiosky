import com.android.apksig.ApkSigner;
import com.android.apksig.SigningCertificateLineage;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

/**
 * Signs Kiosky with key rotation from Android 9 (SDK 28): v1/v2 with the legacy key,
 * which Android 5.1–8.1 know, and v3 with the release key plus the proof of rotation
 * (signing/kiosky.lineage). uber-apk-signer can't choose the rotation SDK, and the
 * signing library's default only rotates on Android 13+.
 *
 *   java -cp .tools/signer.jar tools/Sign.java <aligned.apk> <out.apk> <lineage>
 *
 * Keys come from the KIOSKY_* variables (see ~/.kiosky/release.env).
 */
public class Sign {
    public static void main(String[] args) throws Exception {
        SigningCertificateLineage lineage = SigningCertificateLineage.readFromFile(new File(args[2]));
        ApkSigner.SignerConfig legacy = signer("legacy", env("KIOSKY_LEGACY_KEYSTORE"), env("KIOSKY_LEGACY_ALIAS"), env("KIOSKY_LEGACY_PASS"));
        ApkSigner.SignerConfig release = signer("release", env("KIOSKY_KEYSTORE"), env("KIOSKY_KEY_ALIAS"), env("KIOSKY_KEYSTORE_PASS"));
        new ApkSigner.Builder(Arrays.asList(legacy, release))
                .setInputApk(new File(args[0]))
                .setOutputApk(new File(args[1]))
                .setSigningCertificateLineage(lineage)
                .setMinSdkVersionForRotation(28)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setCreatedBy("Kiosky build.sh")
                .build()
                .sign();
        System.out.println("Signed " + args[1]);
    }

    private static ApkSigner.SignerConfig signer(String name, String path, String alias, String pass) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream in = new FileInputStream(path)) {
            ks.load(in, pass.toCharArray());
        }
        PrivateKey key = (PrivateKey) ks.getKey(alias, pass.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        if (key == null || cert == null) throw new IllegalStateException("No key " + alias + " in " + path);
        return new ApkSigner.SignerConfig.Builder(name, key, Collections.singletonList(cert)).build();
    }

    private static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) throw new IllegalStateException(name + " isn't set (source ~/.kiosky/release.env)");
        return v.replace("$HOME", System.getProperty("user.home"));
    }
}
