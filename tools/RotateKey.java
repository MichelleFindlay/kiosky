import com.android.apksig.SigningCertificateLineage;
import com.android.apksig.SigningCertificateLineage.SignerCapabilities;
import com.android.apksig.SigningCertificateLineage.SignerConfig;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Creates the APK signature scheme v3 proof of rotation: the legacy key vouches for
 * the release key, so Android 9+ tablets accept updates signed with the new key and,
 * once updated, stop accepting the old one (it gets no "rollback" capability).
 *
 * Run once, with the variables from ~/.kiosky/release.env set:
 *   java -cp .tools/signer.jar tools/RotateKey.java signing/kiosky.lineage
 *
 * The lineage file holds only certificates and signatures, nothing secret, so it's
 * committed; build.sh passes it to the signer.
 */
public class RotateKey {
    public static void main(String[] args) throws Exception {
        SignerConfig legacy = signer(env("KIOSKY_LEGACY_KEYSTORE"), env("KIOSKY_LEGACY_ALIAS"), env("KIOSKY_LEGACY_PASS"));
        SignerConfig release = signer(env("KIOSKY_KEYSTORE"), env("KIOSKY_KEY_ALIAS"), env("KIOSKY_KEYSTORE_PASS"));
        SignerCapabilities oldCaps = new SignerCapabilities.Builder()
                .setInstalledData(true)   // keep settings and sign-ins through the update
                .setSharedUid(true)
                .setPermission(true)      // keep granted permissions (and device owner)
                .setRollback(false)       // never accept updates signed only with the old key again
                .setAuth(true)
                .build();
        SigningCertificateLineage lineage = new SigningCertificateLineage.Builder(legacy, release)
                .setMinSdkVersion(28)     // Android 9: the first with key rotation
                .setOriginalCapabilities(oldCaps)
                .setNewCapabilities(new SignerCapabilities.Builder().build())
                .build();
        File out = new File(args.length > 0 ? args[0] : "signing/kiosky.lineage");
        lineage.writeToFile(out);
        System.out.println("Wrote " + out + " (" + lineage.getCertificatesInLineage().size() + " certificates)");
    }

    private static SignerConfig signer(String path, String alias, String pass) throws Exception {
        KeyStore ks = KeyStore.getInstance(path.endsWith(".jks") ? "JKS" : KeyStore.getDefaultType());
        try (FileInputStream in = new FileInputStream(path)) {
            ks.load(in, pass.toCharArray());
        }
        PrivateKey key = (PrivateKey) ks.getKey(alias, pass.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        if (key == null || cert == null) throw new IllegalStateException("No key " + alias + " in " + path);
        return new SignerConfig.Builder(key, cert).build();
    }

    private static String env(String name) {
        String v = System.getenv(name);
        if (v == null || v.isEmpty()) throw new IllegalStateException(name + " isn't set (source ~/.kiosky/release.env)");
        return v.replace("$HOME", System.getProperty("user.home"));
    }
}
