package com.explodenot.nardol;

import com.explodenot.nardol.cli.NardolCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DocsConsistencyTest {

    @Test
    void testEverySourceClassAppearsInTechnicalDoc() throws IOException {
        Path technicalDoc = Path.of("TECHNICAL.md");
        assertThat(Files.exists(technicalDoc)).isTrue();
        String content = Files.readString(technicalDoc);

        List<Path> javaFiles = Files.walk(Path.of("src/main/java"))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .toList();

        for (Path f : javaFiles) {
            String className = f.getFileName().toString().replace(".java", "");
            assertThat(content)
                .withFailMessage("Class " + className + " (" + f + ") not found in TECHNICAL.md repository map")
                .contains(className);
        }
    }

    @Test
    void testAllSubcommandsDocumentedInReadme() throws IOException {
        Path readme = Path.of("README.md");
        assertThat(Files.exists(readme)).isTrue();
        String content = Files.readString(readme);

        CommandLine cmd = new CommandLine(new NardolCommand());
        for (String subName : cmd.getSubcommands().keySet()) {
            assertThat(content)
                .withFailMessage("Subcommand '" + subName + "' is registered in NardolCommand but missing in README.md")
                .contains("`" + subName + "`");
        }
    }

    @Test
    void testMermaidFencesAreValidAndClosed() throws IOException {
        Path technicalDoc = Path.of("TECHNICAL.md");
        List<String> lines = Files.readAllLines(technicalDoc);

        boolean inMermaid = false;
        int mermaidBlockCount = 0;
        String firstLineOfBlock = "";

        Set<String> validKeywords = Set.of(
            "flowchart", "graph", "sequenceDiagram", "stateDiagram", "stateDiagram-v2", "classDiagram", "erDiagram"
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("```mermaid")) {
                assertThat(inMermaid).withFailMessage("Nested or unclosed mermaid fence at line " + (i + 1)).isFalse();
                inMermaid = true;
                mermaidBlockCount++;
                firstLineOfBlock = "";
            } else if (line.equals("```") && inMermaid) {
                inMermaid = false;
                assertThat(firstLineOfBlock).withFailMessage("Empty mermaid block ending at line " + (i + 1)).isNotBlank();
            } else if (inMermaid && firstLineOfBlock.isEmpty() && !line.isEmpty()) {
                firstLineOfBlock = line;
                String keyword = line.split("\\s+")[0];
                assertThat(validKeywords)
                    .withFailMessage("Invalid diagram keyword '" + keyword + "' at line " + (i + 1))
                    .contains(keyword);
            }
        }

        assertThat(inMermaid).withFailMessage("Unclosed mermaid block at end of file").isFalse();
        assertThat(mermaidBlockCount).isGreaterThanOrEqualTo(10);
    }
}
