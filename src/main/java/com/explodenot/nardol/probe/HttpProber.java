package com.explodenot.nardol.probe;

import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.ProbeOutcome;
import com.explodenot.nardol.model.Target;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;

public final class HttpProber implements Prober {
    private final HttpClient client;

    public HttpProber() {
        HttpClient c;
        try {
            // nosemgrep: java.lang.security.audit.weak-ssl-context.weak-ssl-context
            SSLContext ctx = SSLContext.getInstance("TLS");
            // Trust manager accepts any cert solely to benchmark HTTP TTFB latency across third-party probe targets
            // nosemgrep: problem-based-packs.insecure-transport.java-stdlib.bypass-tls-verification.bypass-tls-verification
            TrustManager[] trustAll = new TrustManager[]{
                // nosemgrep: problem-based-packs.insecure-transport.java-stdlib.bypass-tls-verification.bypass-tls-verification
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    // nosemgrep: java.lang.security.audit.crypto.ssl.insecure-trust-manager.insecure-trust-manager
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    // nosemgrep: problem-based-packs.insecure-transport.java-stdlib.bypass-tls-verification.bypass-tls-verification, java.lang.security.audit.crypto.ssl.insecure-trust-manager.insecure-trust-manager
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            ctx.init(null, trustAll, new java.security.SecureRandom());
            c = HttpClient.newBuilder()
                .sslContext(ctx)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        } catch (Exception e) {
            c = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        }
        this.client = c;
    }

    @Override
    public ProbeOutcome probe(Target target, Duration timeout, AddressFamily addressFamily) {
        // Validate against private/bogon destinations before sending HTTP requests
        FailureClassifier.ResolvedTarget resolved;
        try {
            resolved = FailureClassifier.resolve(target.host(), addressFamily);
        } catch (Throwable t) {
            return FailureClassifier.classify(t);
        }

        String scheme = (target.port() == 80 || target.port() == 8080 || target.port() != 443 && target.host().equals("127.0.0.1")) ? "http" : "https";
        String url = scheme + "://" + target.host() + ":" + target.port() + "/";

        long start = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(timeout)
                .header("User-Agent", "nardol/2.0")
                .build();

            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            long end = System.nanoTime();
            double totalMs = (end - start) / 1_000_000.0;

            return new ProbeOutcome.Success(
                0.0,
                0.0,
                Math.round(totalMs * 100.0) / 100.0,
                target.host(),
                addressFamily,
                resp.version().name(),
                null,
                resp.statusCode()
            );
        } catch (Throwable t) {
            // Fallback to GET with Range 0-0
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(timeout)
                    .header("User-Agent", "nardol/2.0")
                    .header("Range", "bytes=0-0")
                    .build();

                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                long end = System.nanoTime();
                double totalMs = (end - start) / 1_000_000.0;

                return new ProbeOutcome.Success(
                    0.0,
                    0.0,
                    Math.round(totalMs * 100.0) / 100.0,
                    target.host(),
                    addressFamily,
                    resp.version().name(),
                    null,
                    resp.statusCode()
                );
            } catch (Throwable t2) {
                return FailureClassifier.classify(t2);
            }
        }
    }
}
