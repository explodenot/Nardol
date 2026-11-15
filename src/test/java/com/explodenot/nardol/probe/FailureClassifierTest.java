package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class FailureClassifierTest {

    @Test
    void testClassifyUnknownHost() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new UnknownHostException("bad.host"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.DnsFailure.class);
    }

    @Test
    void testClassifyTimeout() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new SocketTimeoutException("connect timed out"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.Timeout.class);
    }

    @Test
    void testClassifyRefused() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new ConnectException("Connection refused"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.Refused.class);
    }

    @Test
    void testClassifyUnreachable() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new NoRouteToHostException("No route to host"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.Unreachable.class);
    }

    @Test
    void testClassifyTlsFailure() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new javax.net.ssl.SSLHandshakeException("handshake failure"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.TlsFailure.class);
    }

    @Test
    void testControlChecks() {
        ControlChecks.Diagnostic d = ControlChecks.run();
        assertThat(d).isNotNull();
    }

    @Test
    void testBogonAndPrivateDetection() throws Exception {
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("10.1.2.3"))).isTrue();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("192.168.1.1"))).isTrue();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("172.16.0.1"))).isTrue();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("::1"))).isTrue();

        // Public IPs should not be bogon/private
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("1.1.1.1"))).isFalse();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("8.8.8.8"))).isFalse();
        assertThat(FailureClassifier.isBogonOrPrivate(java.net.InetAddress.getByName("9.9.9.9"))).isFalse();
    }

    @Test
    void testClassifyBlocked() {
        ProbeOutcome.Failure f = FailureClassifier.classify(new SecurityException("Destination resolves to private/bogon IP address: 127.0.0.1"));
        assertThat(f).isInstanceOf(ProbeOutcome.Failure.Blocked.class);
        assertThat(f.message()).contains("private/bogon");
    }
}
