package com.explodenot.nardol.list;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;

public final class SignatureVerifier {
    private SignatureVerifier() {}

    public static PublicKey loadBundledPublicKey() {
        try (InputStream in = SignatureVerifier.class.getResourceAsStream("/targets-public.key")) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean verifyEd25519(byte[] content, byte[] signature, PublicKey publicKey) {
        if (content == null || signature == null || publicKey == null) return false;
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(content);
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean verifySha256(byte[] content, String expectedSha256Hex) {
        if (content == null || expectedSha256Hex == null) return false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            String actualHex = HexFormat.of().formatHex(digest);
            return actualHex.equalsIgnoreCase(expectedSha256Hex.trim().split("\\s+")[0]);
        } catch (Exception e) {
            return false;
        }
    }
}
