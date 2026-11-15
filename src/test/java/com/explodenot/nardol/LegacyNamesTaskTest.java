package com.explodenot.nardol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyNamesTaskTest {

    private static final List<Pattern> FORBIDDEN = List.of(
        Pattern.compile("globalping[-_ ]?lite", Pattern.CASE_INSENSITIVE),
        Pattern.compile("com\\.example\\.gpl", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bgpl\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bGpl[A-Z]\\w*"),
        Pattern.compile("\\bGPL_\\w+")
    );

    private static final Pattern ALLOWED_GLOBALPING_IO = Pattern.compile("globalping\\.io", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALLOWED_GPL_LICENSE = Pattern.compile("GNU General Public License", Pattern.CASE_INSENSITIVE);

    private boolean hasViolation(String line) {
        if (ALLOWED_GLOBALPING_IO.matcher(line).find() || ALLOWED_GPL_LICENSE.matcher(line).find()) {
            return false;
        }
        for (Pattern p : FORBIDDEN) {
            if (p.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    @Test
    void testPlantedLegacyTokenFails(@TempDir Path tempDir) throws IOException {
        Path plantedFile = tempDir.resolve("bad-file.txt");
        Files.writeString(plantedFile, "This is an old gpl binary reference");

        boolean violation = Files.readAllLines(plantedFile).stream().anyMatch(this::hasViolation);
        assertThat(violation).isTrue();
    }

    @Test
    void testAllowlistedTokensPass(@TempDir Path tempDir) throws IOException {
        Path allowedFile = tempDir.resolve("good-file.txt");
        Files.writeString(allowedFile, "Comparison against globalping.io network and GNU General Public License terms");

        boolean violation = Files.readAllLines(allowedFile).stream().anyMatch(this::hasViolation);
        assertThat(violation).isFalse();
    }

    @Test
    void testJarManifestAndEntries() throws IOException {
        Path jarPath = Path.of("build/libs/nardol-2.1.0-all.jar");
        if (!Files.exists(jarPath)) return;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String title = jar.getManifest().getMainAttributes().getValue("Implementation-Title");
            assertThat(title).isEqualTo("nardol");

            boolean hasOldPackage = jar.stream().anyMatch(e -> e.getName().startsWith("com/example/gpl/"));
            assertThat(hasOldPackage).isFalse();
        }
    }
}
