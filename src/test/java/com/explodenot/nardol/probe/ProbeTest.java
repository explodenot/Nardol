package com.explodenot.nardol.probe;

import com.explodenot.nardol.history.HistoryStore;
import com.explodenot.nardol.model.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeTest {
    private static ServerSocket echoServer;
    private static int echoPort;
    private static HttpServer httpServer;
    private static int httpPort;

    @BeforeAll
    static void setup() throws Exception {
        // Local test echo server for mock TCP connect testing
        // nosemgrep
        echoServer = new ServerSocket(0);
        echoPort = echoServer.getLocalPort();
        Thread.ofVirtual().start(() -> {
            while (!echoServer.isClosed()) {
                try {
                    var s = echoServer.accept();
                    s.close();
                } catch (Exception ignored) {}
            }
        });

        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        httpServer.start();
        httpPort = httpServer.getAddress().getPort();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (echoServer != null) echoServer.close();
        if (httpServer != null) httpServer.stop(0);
    }

    @Test
    void testTcpProberSuccess() {
        TcpProber prober = new TcpProber();
        Target t = new Target("local", "Test", "US", "Local", "test", "127.0.0.1", echoPort, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(t, Duration.ofSeconds(2), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Success.class);
        ProbeOutcome.Success s = (ProbeOutcome.Success) outcome;
        assertThat(s.totalMs()).isGreaterThan(0.0);
    }

    @Test
    void testTcpProberRefused() {
        TcpProber prober = new TcpProber();
        Target t = new Target("local", "Test", "US", "Local", "test", "127.0.0.1", 1, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(t, Duration.ofSeconds(1), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Failure.Refused.class);
    }

    @Test
    void testDnsFailure() {
        TcpProber prober = new TcpProber();
        Target t = new Target("local", "Test", "US", "Local", "test", "nonexistent.invalid", 443, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(t, Duration.ofSeconds(1), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Failure.DnsFailure.class);
    }

    @Test
    void testHttpProberSuccess() {
        HttpProber prober = new HttpProber();
        Target t = new Target("local", "Test", "US", "Local", "test", "127.0.0.1", httpPort, 0, 0, Tier.PRIMARY);
        ProbeOutcome outcome = prober.probe(t, Duration.ofSeconds(2), AddressFamily.AUTO);

        assertThat(outcome).isInstanceOf(ProbeOutcome.Success.class);
        ProbeOutcome.Success s = (ProbeOutcome.Success) outcome;
        assertThat(s.httpStatusCode()).isEqualTo(200);
    }

    @Test
    void testProbeRunnerFallbackGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("nardol-test-history");
        HistoryStore store = new HistoryStore(tempDir.resolve("history.jsonl"), 10);

        // Group where primary fails (port 1) and fallback succeeds (echoPort)
        Target primary = new Target("reg1", "NA", "US", "City1", "p1", "127.0.0.1", 1, 38.0, -77.0, Tier.PRIMARY);
        Target fallback = new Target("reg1", "NA", "US", "City1", "p2", "127.0.0.1", echoPort, 38.0, -77.0, Tier.FALLBACK);
        RegionGroup group = new RegionGroup(Region.fromTarget(primary), List.of(primary, fallback));

        TcpProber prober = new TcpProber();
        ProbeRunner runner = new ProbeRunner(prober, store, 38.0, -77.0);

        List<RegionResult> results = runner.run(List.of(group), 2, Duration.ofSeconds(1), 4, AddressFamily.AUTO, false, false, null);

        assertThat(results).hasSize(1);
        RegionResult r = results.get(0);
        assertThat(r.reachable()).isTrue();
        assertThat(r.winningTarget().port()).isEqualTo(echoPort);
        assertThat(r.primaryFailureMessage()).isNotNull();
    }
}
