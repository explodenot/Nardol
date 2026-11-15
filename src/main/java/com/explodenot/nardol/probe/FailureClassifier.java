package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;

public final class FailureClassifier {
    private FailureClassifier() {}

    public record ResolvedTarget(
        InetAddress address,
        double dnsMs,
        AddressFamily family
    ) {}

    public static ResolvedTarget resolve(String host, AddressFamily family) throws UnknownHostException {
        long start = System.nanoTime();
        InetAddress[] addresses = InetAddress.getAllByName(host);
        long end = System.nanoTime();
        double dnsMs = (end - start) / 1_000_000.0;

        List<InetAddress> candidates = Arrays.asList(addresses);

        InetAddress chosen = null;
        if (family == AddressFamily.IPV4) {
            chosen = candidates.stream().filter(a -> a instanceof Inet4Address).findFirst().orElse(null);
            if (chosen == null) {
                throw new UnknownHostException("No IPv4 address resolved for " + host);
            }
        } else if (family == AddressFamily.IPV6) {
            chosen = candidates.stream().filter(a -> a instanceof Inet6Address).findFirst().orElse(null);
            if (chosen == null) {
                throw new UnknownHostException("No IPv6 address resolved for " + host);
            }
        } else {
            // Auto / OS preferred
            chosen = addresses[0];
        }

        AddressFamily actualFamily = (chosen instanceof Inet6Address) ? AddressFamily.IPV6 : AddressFamily.IPV4;
        return new ResolvedTarget(chosen, Math.round(dnsMs * 100.0) / 100.0, actualFamily);
    }

    public static ProbeOutcome.Failure classify(Throwable t) {
        String msg = (t.getMessage() != null) ? t.getMessage() : t.toString();
        String lower = msg.toLowerCase();

        if (t instanceof UnknownHostException) {
            return new ProbeOutcome.Failure.DnsFailure(msg);
        }
        if (t instanceof SocketTimeoutException || lower.contains("timed out") || lower.contains("timeout")) {
            return new ProbeOutcome.Failure.Timeout(msg);
        }
        if (t instanceof ConnectException || lower.contains("refused") || lower.contains("connection refused")) {
            return new ProbeOutcome.Failure.Refused(msg);
        }
        if (t instanceof NoRouteToHostException || lower.contains("no route to host") || lower.contains("unreachable") || lower.contains("network is unreachable")) {
            return new ProbeOutcome.Failure.Unreachable(msg);
        }
        if (lower.contains("ssl") || lower.contains("tls") || lower.contains("handshake") || lower.contains("certificate")) {
            return new ProbeOutcome.Failure.TlsFailure(msg);
        }

        return new ProbeOutcome.Failure.Other(msg);
    }
}
