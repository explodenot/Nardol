package com.explodenot.nardol.list;

import com.explodenot.nardol.model.RegionGroup;
import com.explodenot.nardol.model.Tier;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListExtendedTest {

    @Test
    void testTargetValidatorBoundaries() {
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isTrue();
        assertThat(TargetValidator.validate(null, "NA", "US", "Ash", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("u", "NA", "US", "Ash", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse(); // too short
        assertThat(TargetValidator.validate("us-east", "", "US", "Ash", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "", "Ash", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "", "aws", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "", "host.com", 80, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 0, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 65536, 0.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 80, -91.0, 0.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 80, 0.0, 181.0, "primary").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host.com", 80, 0.0, 0.0, "invalid-tier").valid()).isFalse();
        assertThat(TargetValidator.validate("us-east", "NA", "US", "Ash", "aws", "host/path", 80, 0.0, 0.0, "primary").valid()).isFalse();
    }

    @Test
    void testTargetListLoaderLocalFile() throws Exception {
        Path tempFile = Files.createTempFile("targets", ".tsv");
        String content = "us-test\tNA\tUS\tCity\taws\tdynamodb.us-east-1.amazonaws.com\t443\t30.0\t-70.0\tprimary\n";
        Files.writeString(tempFile, content);

        TargetListLoader loader = new TargetListLoader();
        TargetListLoader.LoadResult res = loader.load(tempFile.toString(), null, null, true, true, true, false);

        assertThat(res.groups()).hasSize(1);
        assertThat(res.sourceName()).contains("local file");
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testTargetListLoaderNetworkFallbackToBundled() throws Exception {
        RemoteFetcher failingFetcher = (url, ifNoneMatchEtag, timeout, allowInsecure) -> {
            throw new IOException("Simulated network outage");
        };

        Path tempCacheDir = Files.createTempDirectory("empty-cache");
        ListCache cache = new ListCache(tempCacheDir);
        TargetListLoader loader = new TargetListLoader(failingFetcher, cache);

        TargetListLoader.LoadResult res = loader.load(null, "https://example.com/unavailable", null, false, false, false, false);

        assertThat(res.groups()).isNotEmpty();
        assertThat(res.sourceName()).contains("bundled fallback list");
        assertThat(res.warnings()).isNotEmpty();
    }

    @Test
    void testListCacheSaveAndLoad() throws Exception {
        Path tempDir = Files.createTempDirectory("test-list-cache");
        ListCache cache = new ListCache(tempDir);
        assertThat(cache.load()).isNull();

        byte[] sample = "content".getBytes();
        cache.save(sample, "\"etag-123\"");
        ListCache.CachedEntry loaded = cache.load();
        assertThat(loaded).isNotNull();
        assertThat(loaded.etag()).isEqualTo("\"etag-123\"");
        assertThat(loaded.content()).isEqualTo(sample);
        assertThat(loaded.ageString()).isEqualTo("<1h old");
    }
}
