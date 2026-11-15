package com.explodenot.nardol.list;

import com.explodenot.nardol.util.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public final class ListCache {
    private final Path cacheDir;

    public ListCache() {
        this(Platform.cacheDir());
    }

    public ListCache(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public record CachedEntry(
        byte[] content,
        String etag,
        Instant lastModified
    ) {
        public String ageString() {
            if (lastModified == null) return "unknown age";
            long hours = Duration.between(lastModified, Instant.now()).toHours();
            if (hours < 1) return "<1h old";
            if (hours < 24) return hours + "h old";
            return (hours / 24) + "d old";
        }

        public boolean isOlderThanHours(long hoursThreshold) {
            if (lastModified == null) return true;
            return Duration.between(lastModified, Instant.now()).toHours() >= hoursThreshold;
        }
    }

    public void save(byte[] content, String etag) {
        try {
            Files.createDirectories(cacheDir);
            Path dataFile = cacheDir.resolve("targets.tsv");
            Path etagFile = cacheDir.resolve("targets.etag");
            Files.write(dataFile, content);
            if (etag != null) {
                Files.writeString(etagFile, etag);
            }
        } catch (IOException ignored) {}
    }

    public CachedEntry load() {
        try {
            Path dataFile = cacheDir.resolve("targets.tsv");
            if (!Files.exists(dataFile)) return null;
            byte[] content = Files.readAllBytes(dataFile);
            String etag = null;
            Path etagFile = cacheDir.resolve("targets.etag");
            if (Files.exists(etagFile)) {
                etag = Files.readString(etagFile).trim();
            }
            Instant modTime = Files.getLastModifiedTime(dataFile).toInstant();
            return new CachedEntry(content, etag, modTime);
        } catch (IOException e) {
            return null;
        }
    }
}
