package com.explodenot.gli.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CliTest {

    @Test
    void testVersionOutput() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new NardolCommand());
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("--version");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
        assertThat(sw.toString()).contains("Nardol v2.0.0");
    }

    @Test
    void testHelpOutput() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new NardolCommand());
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("--help");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
        assertThat(sw.toString()).contains("Nardol: Light the beacons, see who answers");
        assertThat(sw.toString()).contains("Commands:");
        assertThat(sw.toString()).contains("run");
        assertThat(sw.toString()).contains("tui");
        assertThat(sw.toString()).contains("map");
        assertThat(sw.toString()).contains("doctor");
    }

    @Test
    void testDoctorCommand() {
        CommandLine cmd = new CommandLine(new NardolCommand());
        int code = cmd.execute("doctor");
        assertThat(code).isIn(ExitCodes.SUCCESS, ExitCodes.ALL_DOWN_OR_CONTROL_FAIL);
    }

    @Test
    void testListValidateSubcommand() throws Exception {
        CommandLine cmd = new CommandLine(new NardolCommand());
        int code = cmd.execute("list", "validate", "src/main/resources/targets.tsv");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
    }

    @Test
    void testListSourcesSubcommand() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new NardolCommand());
        cmd.setOut(new PrintWriter(sw));
        int code = cmd.execute("list", "sources");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
    }

    @Test
    void testHtmlReportGenerationOffline() throws Exception {
        Path tempHtml = Files.createTempFile("nardol-report", ".html");
        CommandLine cmd = new CommandLine(new NardolCommand());
        int code = cmd.execute("--offline", "--region", "na-us-east-va", "--samples", "1", "--timeout", "1000", "report", tempHtml.toString());

        assertThat(Files.exists(tempHtml)).isTrue();
        String content = Files.readString(tempHtml);
        assertThat(content).contains("Nardol Reachability Report");
        assertThat(content).contains("<svg class=\"world-map\"");
        assertThat(content).contains("application/json");
        Files.deleteIfExists(tempHtml);
    }

    @Test
    void testJsonOutputPipeClean() {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        try {
            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(err));
            CommandLine cmd = new CommandLine(new NardolCommand());

            int code = cmd.execute("--json", "--offline", "--region", "na-us-east-va", "--samples", "1", "--timeout", "1000");
            String stdout = out.toString();

            assertThat(stdout).contains("\"schemaVersion\": \"2.0\"");
            assertThat(stdout).contains("\"results\":");
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
        }
    }

    @Test
    void testCompletionGeneration() {
        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(new NardolCommand());
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("completion", "bash");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
    }
}
