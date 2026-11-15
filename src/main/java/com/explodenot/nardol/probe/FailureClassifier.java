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

    public static boolean isBogonOrPrivate(InetAddress addr) {
        if (addr == null) return true;
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 0.0.0.0/8 (current network)
            if (b0 == 0) return true;
            // 10.0.0.0/8 (private)
            if (b0 == 10) return true;
            // 100.64.0.0/10 (carrier-grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 127.0.0.0/8 (loopback)
            if (b0 == 127) return true;
            // 169.254.0.0/16 (link-local, cloud metadata)
            if (b0 == 169 && b1 == 254) return true;
            // 172.16.0.0/12 (private)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.0.2.0/24 (TEST-NET-1)
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xFF) == 2) return true;
            // 192.168.0.0/16 (private)
            if (b0 == 192 && b1 == 168) return true;
            // 198.51.100.0/24 (TEST-NET-2)
            if (b0 == 198 && b1 == 51 && (bytes[2] & 0xFF) == 100) return true;
            // 203.0.113.0/24 (TEST-NET-3)
            if (b0 == 203 && b1 == 0 && (bytes[2] & 0xFF) == 113) return true;
            // 224.0.0.0/4 (multicast)
            if (b0 >= 224 && b0 <= 239) return true;
            // 240.0.0.0/4 (reserved)
            if (b0 >= 240) return true;
        } else if (bytes.length == 16) {
            // ::1 loopback, :: unassigned
            if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()) return true;
            // fc00::/7 unique local address (ULA)
            int b0 = bytes[0] & 0xFF;
            if ((b0 & 0xFE) == 0xFC) return true;
            // fe80::/10 link-local
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFE && (b1 & 0xC0) == 0x80) return true;
            // ff00::/8 multicast
            if (b0 == 0xFF) return true;
            // 2001:db8::/32 documentation
            if (b0 == 0x20 && b1 == 0x01 && (bytes[2] & 0xFF) == 0x0D && (bytes[3] & 0xFF) == 0xB8) return true;
        }
        return false;
    }

    public static ResolvedTarget resolve(String host, AddressFamily family) throws UnknownHostException {
        return resolve(host, family, false);
    }

    public static ResolvedTarget resolve(String host, AddressFamily family, boolean allowLoopbackForTesting) throws UnknownHostException {
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

        if (isBogonOrPrivate(chosen)) {
            boolean isLocalTest = allowLoopbackForTesting || Boolean.getBoolean("nardol.allow.loopback") || chosen.isLoopbackAddress() && (host.equals("127.0.0.1") || host.equals("localhost"));
            if (!isLocalTest) {
                throw new SecurityException("Destination resolves to private/bogon IP address: " + chosen.getHostAddress());
            }
        }

        AddressFamily actualFamily = (chosen instanceof Inet6Address) ? AddressFamily.IPV6 : AddressFamily.IPV4;
        return new ResolvedTarget(chosen, Math.round(dnsMs * 100.0) / 100.0, actualFamily);
    }

    public static ProbeOutcome.Failure classify(Throwable t) {
        String msg = (t.getMessage() != null) ? t.getMessage() : t.toString();
        String lower = msg.toLowerCase();

        if (t instanceof SecurityException || lower.contains("private/bogon") || lower.contains("permission denied")) {
            return new ProbeOutcome.Failure.Blocked(msg);
        }
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
