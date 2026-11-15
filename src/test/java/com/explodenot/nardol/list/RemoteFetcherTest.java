package com.explodenot.nardol.list;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteFetcherTest {

    @Test
    void testInsecureUrlRejection() {
        RemoteFetcher fetcher = RemoteFetcher.DEFAULT;
        assertThatThrownBy(() -> fetcher.fetch("http://insecure.example.com", null, Duration.ofSeconds(1), false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insecure HTTP list URL rejected");
    }

    @Test
    void testLargeListSizeExceeded() {
        String huge = "a".repeat(TsvParser.MAX_FILE_SIZE_BYTES + 10);
        assertThatThrownBy(() -> TsvParser.parse(huge))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maximum size limit");

        assertThatThrownBy(() -> JsonListParser.parse(huge))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maximum size limit");
    }
}
