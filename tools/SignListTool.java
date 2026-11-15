import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class SignListTool {
    public static void main(String[] args) throws Exception {
        Path keyDir = Path.of("tools/keys");
        Files.createDirectories(keyDir);
        Path pubKeyFile = Path.of("src/main/resources/targets-public.key");
        Path privKeyFile = keyDir.resolve("ed25519-private.key");

        KeyPair kp;
        String envKeyBase64 = System.getenv("NARDOL_ED25519_PRIVATE_KEY");

        if (envKeyBase64 != null && !envKeyBase64.isBlank()) {
            byte[] privBytes = Base64.getDecoder().decode(envKeyBase64.trim());
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("Ed25519");
            PrivateKey priv = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privBytes));
            PublicKey pub = Files.exists(pubKeyFile)
                ? kf.generatePublic(new java.security.spec.X509EncodedKeySpec(Files.readAllBytes(pubKeyFile)))
                : null;
            kp = new KeyPair(pub, priv);
            System.out.println("Using Ed25519 private key from NARDOL_ED25519_PRIVATE_KEY environment variable.");
        } else if (!Files.exists(privKeyFile)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            kp = kpg.generateKeyPair();
            Files.write(privKeyFile, kp.getPrivate().getEncoded());
            Files.write(pubKeyFile, kp.getPublic().getEncoded());
            System.out.println("Generated new Ed25519 keypair.");
            System.out.println("Base64 Private Key (save as NARDOL_ED25519_PRIVATE_KEY in GitHub Secrets if needed):");
            System.out.println(Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
        } else {
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("Ed25519");
            PrivateKey priv = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Files.readAllBytes(privKeyFile)));
            PublicKey pub = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(Files.readAllBytes(pubKeyFile)));
            kp = new KeyPair(pub, priv);
        }

        Path targetFile = Path.of(args.length > 0 ? args[0] : "src/main/resources/targets.tsv");
        byte[] content = Files.readAllBytes(targetFile);

        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(kp.getPrivate());
        sig.update(content);
        byte[] signatureBytes = sig.sign();

        Path sigFile = Path.of(targetFile.toString() + ".sig");
        Files.write(sigFile, signatureBytes);

        // Also update targets.tsv.sig in project root if targets.tsv in root exists
        if (Files.exists(Path.of("targets.tsv"))) {
            Files.copy(sigFile, Path.of("targets.tsv.sig"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Signed " + targetFile + " -> " + sigFile + " (" + signatureBytes.length + " bytes)");
    }
}
