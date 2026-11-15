package com.explodenot.gli.cli;

import picocli.AutoComplete;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "completion", description = "Generate shell completion script (bash, zsh, fish)", mixinStandardHelpOptions = true)
public class CompletionCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Shell type (bash, zsh, fish)")
    private String shell;

    @Override
    public Integer call() {
        if (shell == null) shell = "bash";
        shell = shell.toLowerCase();

        String script = switch (shell) {
            case "zsh" -> AutoComplete.bash("nardol", new picocli.CommandLine(new NardolCommand()));
            case "fish" -> "# Fish completion for nardol\ncomplete -c nardol -a 'run tui map report history list doctor completion config'";
            default -> AutoComplete.bash("nardol", new picocli.CommandLine(new NardolCommand()));
        };

        System.out.println(script);
        return ExitCodes.SUCCESS;
    }
}
