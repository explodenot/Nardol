package com.explodenot.gli.list;

import com.explodenot.gli.model.RegionGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TargetListLoader {
    public static final String DEFAULT_LIST_URL = "https://raw.githubusercontent.com/globalping-lite/targets/main/targets.tsv";

    public record LoadResult(
        List<RegionGroup> groups,
        String sourceName,
        String sourceAge,
        List<String> warnings
    ) {}

    private final RemoteFetcher fetcher;
    private final ListCache cache;
    private final PublicKey publicKey;

    public TargetListLoader(RemoteFetcher fetcher, ListCache cache) {
        this.fetcher = fetcher;
        this.cache = cache;
        this.publicKey = SignatureVerifier.loadBundledPublicKey();
    }

    public TargetListLoader() {
        this(RemoteFetcher.DEFAULT, new ListCache());
    }

    public LoadResult load(
        String customFile,
        String remoteUrl,
        List<String> mirrors,
        boolean noUpdate,
        boolean offline,
        boolean insecureSkipVerify,
        boolean allowInsecureList
    ) throws IOException {
        List<String> warnings = new ArrayList<>();

        // 1. Explicit local file
        if (customFile != null && !customFile.isBlank()) {
            Path p = Path.of(customFile);
            if (!Files.exists(p)) {
                throw new IOException("Specified list file does not exist: " + customFile);
            }
            String content = Files.readString(p, StandardCharsets.UTF_8);
            TsvParser.ParseResult parsed = parseContent(content);
            return new LoadResult(parsed.groups(), "local file (" + p.getFileName() + ")", "local", parsed.errors());
        }

        // 2. Network update if allowed
        if (!offline && !noUpdate) {
            String url = (remoteUrl != null && !remoteUrl.isBlank()) ? remoteUrl : System.getenv("GPL_LIST_URL");
            if (url == null || url.isBlank()) {
                url = DEFAULT_LIST_URL;
            }

            List<String> candidateUrls = new ArrayList<>();
            candidateUrls.add(url);
            if (mirrors != null) {
                candidateUrls.addAll(mirrors);
            }

            ListCache.CachedEntry cached = cache.load();
            String ifNoneMatch = (cached != null) ? cached.etag() : null;

            for (String targetUrl : candidateUrls) {
                try {
                    RemoteFetcher.FetchResult res = fetcher.fetch(targetUrl, ifNoneMatch, Duration.ofSeconds(4), allowInsecureList);
                    if (res.statusCode() == 304 && cached != null) {
                        TsvParser.ParseResult parsed = parseContent(new String(cached.content(), StandardCharsets.UTF_8));
                        return new LoadResult(parsed.groups(), "cached network copy (304 Not Modified)", cached.ageString(), parsed.errors());
                    } else if (res.statusCode() == 200) {
                        byte[] body = res.body();
                        // Verify signature unless skipped
                        boolean verified = insecureSkipVerify;
                        if (!verified) {
                            // Try fetching signature file
                            try {
                                RemoteFetcher.FetchResult sigRes = fetcher.fetch(targetUrl + ".sig", null, Duration.ofSeconds(4), allowInsecureList);
                                if (sigRes.statusCode() == 200 && SignatureVerifier.verifyEd25519(body, sigRes.body(), publicKey)) {
                                    verified = true;
                                }
                            } catch (Exception ignored) {}

                            // Try sha256 checksum fallback
                            if (!verified) {
                                try {
                                    RemoteFetcher.FetchResult shaRes = fetcher.fetch(targetUrl + ".sha256", null, Duration.ofSeconds(4), allowInsecureList);
                                    if (shaRes.statusCode() == 200 && SignatureVerifier.verifySha256(body, new String(shaRes.body(), StandardCharsets.UTF_8))) {
                                        verified = true;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        if (verified) {
                            cache.save(body, res.etag());
                            TsvParser.ParseResult parsed = parseContent(new String(body, StandardCharsets.UTF_8));
                            return new LoadResult(parsed.groups(), "remote network (" + targetUrl + ")", "fresh", parsed.errors());
                        } else {
                            warnings.add("Remote list verification failed for " + targetUrl + "; falling back to cache/bundled list");
                        }
                    }
                } catch (Exception e) {
                    warnings.add("Failed to fetch list from " + targetUrl + ": " + e.getMessage());
                }
            }
        }

        // 3. Cached copy
        ListCache.CachedEntry cached = cache.load();
        if (cached != null) {
            try {
                TsvParser.ParseResult parsed = parseContent(new String(cached.content(), StandardCharsets.UTF_8));
                if (!parsed.groups().isEmpty()) {
                    warnings.addAll(parsed.errors());
                    return new LoadResult(parsed.groups(), "cached copy", cached.ageString(), warnings);
                }
            } catch (Exception e) {
                warnings.add("Failed to parse cached list: " + e.getMessage());
            }
        }

        // 4. Bundled resource
        try (InputStream in = getClass().getResourceAsStream("/targets.tsv")) {
            if (in == null) {
                throw new IOException("Bundled targets.tsv resource not found");
            }
            byte[] bundledBytes = in.readAllBytes();
            TsvParser.ParseResult parsed = parseContent(new String(bundledBytes, StandardCharsets.UTF_8));
            warnings.addAll(parsed.errors());
            return new LoadResult(parsed.groups(), "bundled fallback list", "v2.0.0", warnings);
        }
    }

    public static TsvParser.ParseResult parseContent(String content) throws IOException {
        String trimmed = content.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return JsonListParser.parse(content);
        } else {
            return TsvParser.parse(content);
        }
    }
}
