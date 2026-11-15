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
            SSLContext ctx = SSLContext.getInstance("TLS");
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
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
