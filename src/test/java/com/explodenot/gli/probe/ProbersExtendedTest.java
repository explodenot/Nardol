package com.explodenot.gli.probe;

import com.explodenot.gli.model.AddressFamily;
import com.explodenot.gli.model.ProbeOutcome;
import com.explodenot.gli.model.Target;
import com.explodenot.gli.model.Tier;
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
