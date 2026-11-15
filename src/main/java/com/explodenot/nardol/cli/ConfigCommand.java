package com.explodenot.nardol.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "config", description = "View and modify user configuration defaults", mixinStandardHelpOptions = true, subcommands = {
    ConfigCommand.ShowSubcommand.class,
    ConfigCommand.SetSubcommand.class,
    ConfigCommand.ResetSubcommand.class
})
public class ConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.err.println("Specify a subcommand: show, set, reset. See --help.");
        return ExitCodes.USAGE_ERROR;
    }

    @Command(name = "show", description = "Show current configuration")
    public static class ShowSubcommand implements Callable<Integer> {
        @Override
        public Integer call() {
            ConfigFile.Config cfg = ConfigFile.load();
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(cfg));
            return ExitCodes.SUCCESS;
        }
    }

    @Command(name = "set", description = "Set a configuration property (e.g. set lat 37.77)")
    public static class SetSubcommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Property key (mode, samples, timeoutMs, concurrency, lat, lon)")
        private String key;

        @Parameters(index = "1", description = "Property value")
        private String value;

        @Override
        public Integer call() throws Exception {
            ConfigFile.Config cfg = ConfigFile.load();
            switch (key.toLowerCase()) {
                case "mode" -> cfg.mode = value;
                case "samples" -> cfg.samples = Integer.parseInt(value);
                case "timeoutms" -> cfg.timeoutMs = Integer.parseInt(value);
                case "concurrency" -> cfg.concurrency = Integer.parseInt(value);
                case "lat" -> cfg.lat = Double.parseDouble(value);
                case "lon" -> cfg.lon = Double.parseDouble(value);
                default -> {
                    System.err.println("Unknown configuration key: " + key);
                    return ExitCodes.USAGE_ERROR;
                }
            }
            ConfigFile.save(cfg);
            System.out.println("Updated " + key + " = " + value);
            return ExitCodes.SUCCESS;
        }
    }

    @Command(name = "reset", description = "Reset configuration to default")
    public static class ResetSubcommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            ConfigFile.reset();
            System.out.println("Configuration reset to defaults.");
            return ExitCodes.SUCCESS;
        }
    }
}
