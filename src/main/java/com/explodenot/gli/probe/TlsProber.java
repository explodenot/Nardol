package com.explodenot.gli.probe;

import com.explodenot.gli.model.AddressFamily;
import com.explodenot.gli.model.ProbeOutcome;
import com.explodenot.gli.model.Target;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;

public final class TlsProber implements Prober {
    private static final SSLSocketFactory SSL_FACTORY;

    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            // Trust manager that accepts any cert since we only measure handshake reachability/timing
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            ctx.init(null, trustAll, new java.security.SecureRandom());
            SSL_FACTORY = ctx.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSLContext", e);
        }
    }

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
        try (Socket plainSocket = new Socket()) {
            plainSocket.connect(new InetSocketAddress(resolved.address(), target.port()), timeoutMs);
            long connectEnd = System.nanoTime();
            double connectMs = (connectEnd - connectStart) / 1_000_000.0;

            // TLS handshake with SNI
            plainSocket.setSoTimeout(timeoutMs);
            try (SSLSocket sslSocket = (SSLSocket) SSL_FACTORY.createSocket(
                plainSocket, target.host(), target.port(), true
            )) {
                SSLParameters params = sslSocket.getSSLParameters();
                params.setServerNames(Collections.singletonList(new SNIHostName(target.host())));
                sslSocket.setSSLParameters(params);

                long tlsStart = System.nanoTime();
                sslSocket.startHandshake();
                long tlsEnd = System.nanoTime();
                double tlsMs = (tlsEnd - tlsStart) / 1_000_000.0;

                SSLSession session = sslSocket.getSession();
                String protocol = session.getProtocol();
                String cipher = session.getCipherSuite();
                double totalMs = resolved.dnsMs() + connectMs + tlsMs;

                return new ProbeOutcome.Success(
                    resolved.dnsMs(),
                    Math.round(connectMs * 100.0) / 100.0,
                    Math.round(totalMs * 100.0) / 100.0,
                    resolved.address().getHostAddress(),
                    resolved.family(),
                    protocol,
                    cipher,
                    -1
                );
            }
        } catch (Throwable t) {
            return FailureClassifier.classify(t);
        }
    }
}
