package com.explodenot.nardol.list;

import com.explodenot.nardol.model.RegionGroup;
import com.explodenot.nardol.model.Tier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListTest {

    @Test
    void testTsvParserValid() throws IOException {
        String tsv = """
            # Header comment
            us-east-va\tNorth America\tUS\tAshburn\taws\tdynamodb.us-east-1.amazonaws.com\t443\t39.04\t-77.49\tprimary
            us-east-va\tNorth America\tUS\tAshburn\thetzner\tash-speed.hetzner.com\t443\t39.04\t-77.49\tfallback
            """;

        TsvParser.ParseResult res = TsvParser.parse(tsv);
        assertThat(res.groups()).hasSize(1);
        RegionGroup g = res.groups().get(0);
        assertThat(g.region().city()).isEqualTo("Ashburn");
        assertThat(g.primary().host()).isEqualTo("dynamodb.us-east-1.amazonaws.com");
        assertThat(g.fallbacks()).hasSize(1);
        assertThat(res.invalidLines()).isEqualTo(0);
    }

    @Test
    void testValidationRejectsInjectionAttempts() throws IOException {
        String hostile = """
            us-east-va\tNorth America\tUS\tAshburn\taws\t`rm -rf /`\t443\t39.04\t-77.49\tprimary
            invalid-port\tNorth America\tUS\tAshburn\taws\thost.com\t99999\t39.04\t-77.49\tprimary
            bad-lat\tNorth America\tUS\tAshburn\taws\thost.com\t443\t190.04\t-77.49\tprimary
            """;

        TsvParser.ParseResult res = TsvParser.parse(hostile);
        assertThat(res.validTargets()).isEqualTo(0);
        assertThat(res.invalidLines()).isEqualTo(3);
    }

    @Test
    void testValidationRejectsBogonIpLiterals() throws IOException {
        String bogon = """
            reg-local\tNorth America\tUS\tAshburn\taws\t127.0.0.1\t443\t39.04\t-77.49\tprimary
            reg-priv\tNorth America\tUS\tAshburn\taws\t10.0.0.1\t443\t39.04\t-77.49\tprimary
            reg-meta\tNorth America\tUS\tAshburn\taws\t169.254.169.254\t443\t39.04\t-77.49\tprimary
            """;

        TsvParser.ParseResult res = TsvParser.parse(bogon);
        assertThat(res.validTargets()).isEqualTo(0);
        assertThat(res.invalidLines()).isEqualTo(3);
    }

    @Test
    void testJsonParser() throws IOException {
        String json = """
            [
              {
                "region_id": "us-east-va",
                "continent": "North America",
                "country": "US",
                "city": "Ashburn",
                "provider": "aws",
                "host": "dynamodb.us-east-1.amazonaws.com",
                "port": 443,
                "lat": 39.04,
                "lon": -77.49,
                "tier": "primary"
              }
            ]
            """;

        TsvParser.ParseResult res = JsonListParser.parse(json);
        assertThat(res.groups()).hasSize(1);
        assertThat(res.validTargets()).isEqualTo(1);
        assertThat(res.groups().get(0).primary().tier()).isEqualTo(Tier.PRIMARY);
    }

    @Test
    void testEd25519Verification() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();

        byte[] data = "test content".getBytes();
        java.security.Signature sig = java.security.Signature.getInstance("Ed25519");
        sig.initSign(kp.getPrivate());
        sig.update(data);
        byte[] signatureBytes = sig.sign();

        assertThat(SignatureVerifier.verifyEd25519(data, signatureBytes, kp.getPublic())).isTrue();
        assertThat(SignatureVerifier.verifyEd25519("tampered".getBytes(), signatureBytes, kp.getPublic())).isFalse();
    }

    @Test
    void testSha256Verification() {
        byte[] data = "hello world".getBytes();
        // sha256 for "hello world" is b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        assertThat(SignatureVerifier.verifySha256(data, "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9")).isTrue();
        assertThat(SignatureVerifier.verifySha256(data, "wrongchecksum")).isFalse();
    }
}
