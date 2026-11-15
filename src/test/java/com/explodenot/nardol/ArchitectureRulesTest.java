package com.explodenot.gli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureRulesTest {

    @Test
    void testPackageDependencyRules() throws IOException {
        Path root = Path.of("src/main/java/com/explodenot/gli");

        // Rule 1: model depends on nothing internal
        checkPackageImports(root.resolve("model"), List.of(
            "com.explodenot.gli.cli",
            "com.explodenot.gli.probe",
            "com.explodenot.gli.analysis",
            "com.explodenot.gli.history",
            "com.explodenot.gli.render",
            "com.explodenot.gli.list"
        ));

        // Rule 2: render never calls probe
        checkPackageImports(root.resolve("render"), List.of(
            "com.explodenot.gli.probe"
        ));

        // Rule 3: util depends on nothing in cli or render
        checkPackageImports(root.resolve("util"), List.of(
            "com.explodenot.gli.cli",
            "com.explodenot.gli.render"
        ));
    }

    private void checkPackageImports(Path pkgDir, List<String> forbiddenPrefixes) throws IOException {
        if (!Files.exists(pkgDir)) return;
        List<Path> files = Files.walk(pkgDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();

        for (Path f : files) {
            List<String> lines = Files.readAllLines(f);
            for (String line : lines) {
                if (line.startsWith("import ")) {
                    for (String forbidden : forbiddenPrefixes) {
                        assertThat(line)
                            .withFailMessage("Architecture violation in " + f + ": imports forbidden " + forbidden)
                            .doesNotContain(forbidden);
                    }
                }
            }
        }
    }
}
