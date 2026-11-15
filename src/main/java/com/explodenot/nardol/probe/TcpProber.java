package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;
import com.explodenot.nardol.model.Target;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

public final class TcpProber implements Prober {
    @Override
    public ProbeOutcome probe(Target target, Duration timeout, AddressFamily addressFamily) {
        FailureClassifier.ResolvedTarget resolved;
        try {
            resolved = FailureClassifier.resolve(target.host(), addressFamily);
        } catch (Throwable t) {
            return FailureClassifier.classify(t);
        }

        int timeoutMs = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        long connectStart = System.nanoTime();
        // Layer-4 TCP reachability & connect latency prober
        try (Socket socket = new Socket()) { // nosemgrep: java.lang.security.audit.crypto.unencrypted-socket.unencrypted-socket
            socket.connect(new InetSocketAddress(resolved.address(), target.port()), timeoutMs);
            long connectEnd = System.nanoTime();
            double connectMs = (connectEnd - connectStart) / 1_000_000.0;
            double totalMs = resolved.dnsMs() + connectMs;

            return new ProbeOutcome.Success(
                resolved.dnsMs(),
                Math.round(connectMs * 100.0) / 100.0,
                Math.round(totalMs * 100.0) / 100.0,
                resolved.address().getHostAddress(),
                resolved.family(),
                null,
                null,
                -1
            );
        } catch (Throwable t) {
            return FailureClassifier.classify(t);
        }
    }
}
