package com.explodenot.nardol.probe;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public final class ControlChecks {
    private ControlChecks() {}

    public record Diagnostic(
        boolean dnsOk,
        boolean rawIpOk,
        boolean proxyDetected,
        String proxyWarning,
        boolean passed
    ) {}

    public static Diagnostic run() {
        boolean dnsOk = false;
        // Check DNS resolution across multiple major public DNS resolver domains:
        // Cloudflare (one.one.one.one), Google (dns.google), Quad9 (dns.quad9.net), Cisco Umbrella (dns.umbrella.com)
        String[] resolverCheckHosts = {
            "one.one.one.one",
            "dns.google",
            "dns.quad9.net",
            "dns.umbrella.com"
        };
        for (String resolverHost : resolverCheckHosts) {
            try {
                InetAddress addr = InetAddress.getByName(resolverHost);
                if (addr != null) {
                    dnsOk = true;
                    break;
                }
            } catch (Exception ignored) {}
        }

        boolean rawIpOk = false;
        // Probe raw IP endpoints across multiple providers (Cloudflare, Google, Quad9, Cisco Umbrella)
        // on port 53 (DNS) and port 443 (HTTPS) across IPv4 and IPv6
        record RawTarget(String host, int port) {}
        RawTarget[] rawTargets = new RawTarget[]{
            new RawTarget("1.1.1.1", 53),
            new RawTarget("1.1.1.1", 443),
            new RawTarget("8.8.8.8", 53),
            new RawTarget("8.8.8.8", 443),
            new RawTarget("9.9.9.9", 53),
            new RawTarget("9.9.9.9", 443),
            new RawTarget("208.67.222.222", 53),
            new RawTarget("208.67.222.222", 443),
            new RawTarget("2606:4700:4700::1111", 53),
            new RawTarget("2001:4860:4860::8888", 53),
            new RawTarget("2620:fe::fe", 53),
            new RawTarget("2620:119:35::35", 53)
        };

        for (RawTarget target : rawTargets) {
            // Raw TCP SYN probe for pre-flight reachability testing
            try (Socket socket = new Socket()) { // nosemgrep: java.lang.security.audit.crypto.unencrypted-socket.unencrypted-socket
                socket.connect(new InetSocketAddress(target.host(), target.port()), 1500);
                rawIpOk = true;
                break;
            } catch (Exception ignored) {}
        }

        // Check proxy variables
        List<String> proxyHints = new ArrayList<>();
        String[] envVars = {"HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "http_proxy", "https_proxy", "all_proxy"};
        for (String ev : envVars) {
            String val = System.getenv(ev);
            if (val != null && !val.isBlank()) {
                proxyHints.add(ev + "=" + val);
            }
        }
        String httpProxyHost = System.getProperty("http.proxyHost");
        if (httpProxyHost != null && !httpProxyHost.isBlank()) {
            proxyHints.add("http.proxyHost=" + httpProxyHost);
        }
        String socksProxyHost = System.getProperty("socksProxyHost");
        if (socksProxyHost != null && !socksProxyHost.isBlank()) {
            proxyHints.add("socksProxyHost=" + socksProxyHost);
        }

        boolean proxyDetected = !proxyHints.isEmpty();
        String warning = proxyDetected ? "Proxy environment/JVM settings detected (" + String.join(", ", proxyHints) + "). Proxies/VPNs/WSL2 NAT may skew latency and route measurements." : null;

        // If at least one check passed or both passed, consider whether local connectivity is broken
        boolean passed = dnsOk || rawIpOk;

        return new Diagnostic(dnsOk, rawIpOk, proxyDetected, warning, passed);
    }
}
