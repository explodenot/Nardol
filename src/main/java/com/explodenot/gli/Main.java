package com.explodenot.gli;

import com.explodenot.gli.cli.ExitCodes;
import com.explodenot.gli.cli.NardolCommand;
import picocli.CommandLine;

public final class Main {
    static {
        // Suppress JLine deprecated terminal provider warning
        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new NardolCommand());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            System.err.println("Error: " + ex.getMessage());
            if (parseResult.hasMatchedOption("--debug")) {
                ex.printStackTrace();
            }
            return ExitCodes.INTERNAL_ERROR;
        });

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
