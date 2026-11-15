package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;
import com.explodenot.nardol.model.Target;
import com.explodenot.nardol.model.Tier;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProbersExtendedTest {

    @Test
    void testTlsProberToKnownTlsServer() {
        TlsProber prober = new TlsProber();
        Target target = new Target("google-dns", "NA", "US", "Any", "google", "dns.google", 443, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(target, Duration.ofSeconds(3), AddressFamily.AUTO);

        if (outcome instanceof ProbeOutcome.Success s) {
            assertThat(s.totalMs()).isGreaterThan(0.0);
            assertThat(s.protocol()).isNotNull();
        }
    }

    @Test
    void testTlsProberConnectionRefused() {
        TlsProber prober = new TlsProber();
        Target target = new Target("local", "NA", "US", "Local", "test", "127.0.0.1", 1, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(target, Duration.ofSeconds(1), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Failure.Refused.class);
    }

    @Test
    void testTlsProberInvalidHost() {
        TlsProber prober = new TlsProber();
        Target target = new Target("local", "NA", "US", "Local", "test", "nonexistent.invalid", 443, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(target, Duration.ofSeconds(1), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Failure.DnsFailure.class);
    }
}
