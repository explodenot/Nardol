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
}
