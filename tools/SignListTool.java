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
        if (!Files.exists(privKeyFile)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            kp = kpg.generateKeyPair();
            Files.write(privKeyFile, kp.getPrivate().getEncoded());
            Files.write(pubKeyFile, kp.getPublic().getEncoded());
            System.out.println("Generated new Ed25519 keypair.");
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
