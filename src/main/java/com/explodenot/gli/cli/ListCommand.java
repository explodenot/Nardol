package com.explodenot.gli.cli;

import com.explodenot.gli.list.TargetListLoader;
import com.explodenot.gli.list.TsvParser;
import com.explodenot.gli.model.RegionGroup;
import com.explodenot.gli.model.Target;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "list", description = "Manage, inspect, and validate the target regions list", mixinStandardHelpOptions = true, subcommands = {
    ListCommand.ShowSubcommand.class,
    ListCommand.UpdateSubcommand.class,
    ListCommand.ValidateSubcommand.class,
    ListCommand.SourcesSubcommand.class
})
public class ListCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.err.println("Specify a subcommand: show, update, validate, sources. See --help.");
        return ExitCodes.USAGE_ERROR;
    }

    @Command(name = "show", description = "Show current regions in the list")
    public static class ShowSubcommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            TargetListLoader loader = new TargetListLoader();
            TargetListLoader.LoadResult res = loader.load(null, null, null, false, false, false, false);
            System.out.printf("Loaded %d regions from %s (%s)%n", res.groups().size(), res.sourceName(), res.sourceAge());
            for (RegionGroup g : res.groups()) {
                System.out.printf("%-18s %-16s %-15s %s (primary: %s)%n",
                    g.region().id(), g.region().continent(), g.region().city(), g.region().country(),
                    g.primary() != null ? g.primary().host() : "none");
            }
            return ExitCodes.SUCCESS;
        }
    }

    @Command(name = "update", description = "Force update the target list from remote repository")
    public static class UpdateSubcommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Updating targets list from remote source...");
            TargetListLoader loader = new TargetListLoader();
            TargetListLoader.LoadResult res = loader.load(null, null, null, false, false, false, false);
            System.out.printf("Targets updated: %d regions loaded from %s%n", res.groups().size(), res.sourceName());
            return ExitCodes.SUCCESS;
        }
    }

    @Command(name = "validate", description = "Lint and validate a candidate target list file")
    public static class ValidateSubcommand implements Callable<Integer> {
        @Parameters(index = "0", description = "File to validate")
        private File file;

        @Override
        public Integer call() throws Exception {
            if (file == null || !file.exists()) {
                System.err.println("File does not exist: " + file);
                return ExitCodes.USAGE_ERROR;
            }
            String content = Files.readString(file.toPath());
            TsvParser.ParseResult res = TargetListLoader.parseContent(content);

            System.out.printf("Validated %s:%n", file.getName());
            System.out.printf("  Total lines/items: %d%n", res.totalLines());
            System.out.printf("  Valid targets: %d%n", res.validTargets());
            System.out.printf("  Distinct regions: %d%n", res.groups().size());
            System.out.printf("  Invalid lines: %d%n", res.invalidLines());

            Set<String> seenHosts = new HashSet<>();
            int dupHosts = 0;
            int missingPrimary = 0;

            for (RegionGroup g : res.groups()) {
                if (g.primary() == null) {
                    System.err.printf("  [WARN] Region %s has no primary candidate!%n", g.region().id());
                    missingPrimary++;
                }
                for (Target t : g.targets()) {
                    if (!seenHosts.add(t.host() + ":" + t.port())) {
                        dupHosts++;
                    }
                }
            }

            if (dupHosts > 0) {
                System.out.printf("  [INFO] Duplicate host:port targets: %d%n", dupHosts);
            }

            if (!res.errors().isEmpty()) {
                System.err.println("Validation errors encountered:");
                for (String err : res.errors()) {
                    System.err.println("  - " + err);
                }
                return ExitCodes.USAGE_ERROR;
            }

            if (missingPrimary > 0 || res.invalidLines() > 0) {
                return ExitCodes.USAGE_ERROR;
            }

            System.out.println("Validation PASSED cleanly!");
            return ExitCodes.SUCCESS;
        }
    }

    @Command(name = "sources", description = "Show target list sources and cache location")
    public static class SourcesSubcommand implements Callable<Integer> {
        @Override
        public Integer call() {
            System.out.println("Default remote URL: " + TargetListLoader.DEFAULT_LIST_URL);
            System.out.println("Cache location:     " + com.explodenot.gli.util.Platform.cacheDir());
            System.out.println("Bundled resource:   /targets.tsv (Ed25519 signed)");
            return ExitCodes.SUCCESS;
        }
    }
}
