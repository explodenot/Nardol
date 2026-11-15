package com.explodenot.gli.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformTest {

    @Test
    void testOsDetection() {
        assertThat(Platform.osName()).isNotBlank();
        boolean any = Platform.isMac() || Platform.isLinux() || Platform.isWindows();
        assertThat(any).isTrue();
    }

    @Test
    void testCacheDir() {
        Path p = Platform.cacheDir();
        assertThat(p).isNotNull();
        assertThat(p.toString()).contains("nardol");
    }

    @Test
    void testConfigDir() {
        Path p = Platform.configDir();
        assertThat(p).isNotNull();
        assertThat(p.toString()).contains("nardol");
    }

    @Test
    void testTimeProvider() {
        assertThat(TimeProvider.SYSTEM.now()).isNotNull();
        assertThat(TimeProvider.SYSTEM.nanoTime()).isGreaterThan(0);
    }
}
