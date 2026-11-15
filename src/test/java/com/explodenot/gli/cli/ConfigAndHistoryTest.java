package com.explodenot.gli.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigAndHistoryTest {

    @Test
    void testConfigSetAndShow() throws Exception {
        CommandLine cmd = new CommandLine(new NardolCommand());
        int setCode = cmd.execute("config", "set", "samples", "5");
        assertThat(setCode).isEqualTo(ExitCodes.SUCCESS);

        ConfigFile.Config loaded = ConfigFile.load();
        assertThat(loaded.samples).isEqualTo(5);

        int resetCode = cmd.execute("config", "reset");
        assertThat(resetCode).isEqualTo(ExitCodes.SUCCESS);
    }

    @Test
    void testHistoryCommandExecution() {
        CommandLine cmd = new CommandLine(new NardolCommand());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int code = cmd.execute("history");
        assertThat(code).isEqualTo(ExitCodes.SUCCESS);
    }
}
