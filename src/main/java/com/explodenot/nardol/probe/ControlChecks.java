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
        try {
            InetAddress addr = InetAddress.getByName("one.one.one.one");
            dnsOk = (addr != null);
        } catch (Exception ignored) {}

        boolean rawIpOk = false;
        // Probe raw IP 1.1.1.1 on port 53 or 80 with short timeout (no DNS needed)
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("1.1.1.1", 53), 1500);
            rawIpOk = true;
        } catch (Exception ignored) {
            // Try 8.8.8.8:53
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
                rawIpOk = true;
            } catch (Exception ignored2) {}
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
