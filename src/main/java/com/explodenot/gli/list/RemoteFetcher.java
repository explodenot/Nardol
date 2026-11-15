package com.explodenot.gli.list;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public interface RemoteFetcher {
    record FetchResult(
        int statusCode,
        byte[] body,
        String etag
    ) {}

    FetchResult fetch(String url, String ifNoneMatchEtag, Duration timeout, boolean allowInsecure) throws IOException, InterruptedException;

    RemoteFetcher DEFAULT = new RemoteFetcher() {
        private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        @Override
        public FetchResult fetch(String url, String ifNoneMatchEtag, Duration timeout, boolean allowInsecure) throws IOException, InterruptedException {
            if (!allowInsecure && !url.startsWith("https://")) {
                throw new IllegalArgumentException("Insecure HTTP list URL rejected: " + url + " (use --allow-insecure-list to override)");
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET();

            if (ifNoneMatchEtag != null && !ifNoneMatchEtag.isBlank()) {
                reqBuilder.header("If-None-Match", ifNoneMatchEtag);
            }

            HttpResponse<byte[]> resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String etag = resp.headers().firstValue("ETag").orElse(null);
            return new FetchResult(resp.statusCode(), resp.body(), etag);
        }
    };
}
