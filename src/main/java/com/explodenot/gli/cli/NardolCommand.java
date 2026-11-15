package com.explodenot.gli.cli;

import com.explodenot.gli.analysis.CorrelationDetector;
import com.explodenot.gli.analysis.Stats;
import com.explodenot.gli.history.HistoryStore;
import com.explodenot.gli.list.TargetListLoader;
import com.explodenot.gli.model.*;
import com.explodenot.gli.probe.*;
import com.explodenot.gli.render.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;

@Command(
    name = "nardol",
    description = "Nardol: Light the beacons, see who answers (global reachability probe)",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    subcommands = {
        NardolCommand.RunSubcommand.class,
        NardolCommand.TuiSubcommand.class,
        NardolCommand.MapSubcommand.class,
        NardolCommand.ReportSubcommand.class,
        HistoryCommand.class,
        ListCommand.class,
        DoctorCommand.class,
        CompletionCommand.class,
        ConfigCommand.class
    }
)
public class NardolCommand implements Callable<Integer> {

    public static final String BANNER_TEXT = """
         _  _   _   ___ ___   ___  _    \s
        | \\| | /_\\ | _ \\   \\ / _ \\| |   \s
        | .` |/ _ \\|   / |) | (_) | |__ \s
        |_|\\_/_/ \\_\\_|_\\___/ \\___/|____|\s
        Light the beacons, see who answers.""";

    @Option(names = {"--region"}, description = "Filter by region ID or continent (repeatable)")
    List<String> regions;

    @Option(names = {"--exclude"}, description = "Exclude region ID or continent (repeatable)")
    List<String> excludes;

    @Option(names = {"--provider"}, description = "Filter by provider name (aws, oci, vultr, etc.)")
    String provider;

    @Option(names = {"--samples"}, description = "Number of probe samples per target (default: 3)")
    Integer samples;

    @Option(names = {"--timeout"}, description = "Per-probe timeout in milliseconds (default: 3000)")
    Integer timeoutMs;

    @Option(names = {"--concurrency"}, description = "Max concurrent probes (default: 64)")
    Integer concurrency;

    @Option(names = {"--port"}, description = "Override target port")
    Integer portOverride;

    @Option(names = {"--mode"}, description = "Probe mode: tcp, tls, http (default: tcp)")
    String mode;

    @Option(names = {"--ipv4"}, description = "Probe using IPv4 only")
    boolean ipv4;

    @Option(names = {"--ipv6"}, description = "Probe using IPv6 only")
    boolean ipv6;

    @Option(names = {"--prefer-ipv6"}, description = "Prefer IPv6 addresses when resolving")
    boolean preferIpv6;

    @Option(names = {"--dual-stack"}, description = "Probe both IPv4 and IPv6 and compare results")
    boolean dualStack;

    @Option(names = {"--all-candidates"}, description = "Probe all fallback candidates in each group")
    boolean allCandidates;

    @Option(names = {"--lat"}, description = "User latitude for physics floor efficiency")
    Double userLat;

    @Option(names = {"--lon"}, description = "User longitude for physics floor efficiency")
    Double userLon;

    @Option(names = {"--json"}, description = "Output full report in JSON format to stdout")
    boolean jsonOutput;

    @Option(names = {"--ndjson"}, description = "Output newline-delimited JSON per region to stdout")
    boolean ndjsonOutput;

    @Option(names = {"--csv"}, description = "Output results in CSV format to stdout")
    boolean csvOutput;

    @Option(names = {"--compact"}, description = "Compact terminal report")
    boolean compact;

    @Option(names = {"--verbose", "-v"}, description = "Verbose terminal output (p95, loss, cipher)")
    boolean verbose;

    @Option(names = {"--quiet", "-q"}, description = "Quiet mode (no progress animation, suppresses banner)")
    boolean quiet;

    @Option(names = {"--sort"}, description = "Sort by: name, latency, delta, efficiency")
    String sortOrder;

    @Option(names = {"--watch"}, description = "Watch mode: repeat probe every N seconds")
    Integer watchIntervalSeconds;

    @Option(names = {"--count"}, description = "Number of probe iterations in watch mode")
    Integer watchCount;

    @Option(names = {"--no-history"}, description = "Do not append this run to local history")
    boolean noHistory;

    @Option(names = {"--force"}, description = "Force probing even if control checks fail")
    boolean force;

    @Option(names = {"--debug"}, description = "Print debug diagnostics and stack traces")
    boolean debug;

    @Option(names = {"--color"}, description = "Color mode: auto, always, never (default: auto)")
    String colorMode;

    @Option(names = {"--no-color"}, description = "Disable ANSI color output")
    boolean noColor;

    @Option(names = {"--theme"}, description = "Color theme: default, beacon, mono, high-contrast, solarized, nord")
    String themeName;

    @Option(names = {"--ascii"}, description = "Force ASCII glyphs instead of Unicode")
    boolean ascii;

    @Option(names = {"--color-depth"}, description = "Force color depth: 16, 256, truecolor")
    String explicitColorDepth;

    @Option(names = {"--list-file"}, description = "Path to local target list file")
    String listFile;

    @Option(names = {"--list-url"}, description = "Custom remote target list URL")
    String listUrl;

    @Option(names = {"--list-mirror"}, description = "Mirror URLs for list update (repeatable)")
    List<String> listMirrors;

    @Option(names = {"--no-update"}, description = "Do not fetch remote target list updates")
    boolean noUpdate;

    @Option(names = {"--offline"}, description = "Operate purely offline (cached/bundled list only)")
    boolean offline;

    @Option(names = {"--insecure-skip-verify"}, description = "Skip Ed25519 signature / sha256 checksum verification")
    boolean insecureSkipVerify;

    @Option(names = {"--allow-insecure-list"}, description = "Allow non-HTTPS target list URLs")
    boolean allowInsecureList;

    @Override
    public Integer call() throws Exception {
        return runProbes(false, false, null);
    }

    public static void printBanner(PrintStream out, Ansi ansi, boolean quiet, boolean noColor) {
        if (quiet) return;
        boolean isTty = System.console() != null || TerminalHelper.getTerminal() != null;
        if (!isTty) return;

        Rgb ember = new Rgb(243, 156, 18); // Amber/orange accent
        out.println(ansi.bold() + ansi.color(ember, BANNER_TEXT) + ansi.reset());
        out.println();
    }

    public Integer runProbes(boolean tuiMode, boolean mapMode, String reportHtmlFile) throws Exception {
        ConfigFile.Config cfg = ConfigFile.load();

        int effectiveSamples = (samples != null) ? samples : (cfg.samples != null ? cfg.samples : 3);
        int effectiveTimeoutMs = (timeoutMs != null) ? timeoutMs : (cfg.timeoutMs != null ? cfg.timeoutMs : 3000);
        int effectiveConcurrency = (concurrency != null) ? concurrency : (cfg.concurrency != null ? cfg.concurrency : 64);
        String effectiveMode = (mode != null) ? mode : (cfg.mode != null ? cfg.mode : "tcp");
        Double effectiveLat = (userLat != null) ? userLat : (cfg.lat != null ? cfg.lat : parseEnvDouble("NARDOL_LAT"));
        Double effectiveLon = (userLon != null) ? userLon : (cfg.lon != null ? cfg.lon : parseEnvDouble("NARDOL_LON"));
        boolean effectiveAscii = ascii || Boolean.TRUE.equals(cfg.ascii);

        ColorDepth depth = (noColor || "never".equalsIgnoreCase(colorMode))
            ? ColorDepth.NONE
            : ColorDepth.detect(explicitColorDepth, false);

        Ansi ansi = new Ansi(depth);
        Glyphs glyphs = Glyphs.create(effectiveAscii);

        boolean machineReadable = jsonOutput || ndjsonOutput || csvOutput;
        PrintStream humanStream = machineReadable ? System.err : System.out;

        if (!machineReadable && !tuiMode) {
            printBanner(humanStream, ansi, quiet, noColor || depth == ColorDepth.NONE);
        }

        // Control checks first
        ControlChecks.Diagnostic diag = ControlChecks.run();
        if (diag.proxyDetected() && !machineReadable) {
            System.err.println(ansi.color(Rgb.YELLOW, glyphs.warning() + " " + diag.proxyWarning()));
        }
        if (!diag.passed() && !force) {
            System.err.println(ansi.color(Rgb.RED, glyphs.failed() + " Local connectivity looks broken (DNS and raw IP control checks failed)."));
            System.err.println("Use --force to probe anyway, or run 'nardol doctor' to diagnose your connection.");
            return ExitCodes.ALL_DOWN_OR_CONTROL_FAIL;
        }

        // Load targets list
        TargetListLoader loader = new TargetListLoader();
        TargetListLoader.LoadResult listRes;
        try {
            listRes = loader.load(listFile, listUrl, listMirrors, noUpdate, offline, insecureSkipVerify, allowInsecureList);
        } catch (Exception e) {
            System.err.println(ansi.color(Rgb.RED, "Failed to load target list: " + e.getMessage()));
            if (debug) e.printStackTrace();
            return ExitCodes.INTERNAL_ERROR;
        }

        if (!listRes.warnings().isEmpty() && !machineReadable) {
            for (String w : listRes.warnings()) {
                System.err.println(ansi.color(Rgb.YELLOW, "Notice: " + w));
            }
        }

        List<RegionGroup> groups = filterGroups(listRes.groups());
        if (groups.isEmpty()) {
            System.err.println("No matching regions to probe after applying filters.");
            return ExitCodes.USAGE_ERROR;
        }

        // Prober setup
        ProbeMode probeMode = ProbeMode.fromString(effectiveMode);
        Prober prober = switch (probeMode) {
            case TCP -> new TcpProber();
            case TLS -> new TlsProber();
            case HTTP -> new HttpProber();
        };

        HistoryStore historyStore = noHistory ? null : new HistoryStore();
        ProbeRunner runner = new ProbeRunner(prober, historyStore, effectiveLat, effectiveLon);

        AddressFamily fam = AddressFamily.AUTO;
        if (ipv4) fam = AddressFamily.IPV4;
        else if (ipv6) fam = AddressFamily.IPV6;

        long startTime = System.currentTimeMillis();
        ProgressLine progress = new ProgressLine(ansi, glyphs, groups.size(), !machineReadable && !quiet, humanStream);

        // Progress shutdown hook for cursor restore
        Runtime.getRuntime().addShutdownHook(new Thread(progress::stop));

        progress.start();

        List<RegionResult> results;
        try {
            results = runner.run(
                groups,
                effectiveSamples,
                Duration.ofMillis(effectiveTimeoutMs),
                effectiveConcurrency,
                fam,
                allCandidates,
                dualStack,
                res -> progress.record(res.reachable(), res.isDegraded())
            );
        } finally {
            progress.stop();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // Apply sorting
        if (sortOrder != null) {
            results = sortResults(results, sortOrder);
        }

        // Summary metrics
        int total = results.size();
        int reachable = (int) results.stream().filter(RegionResult::reachable).count();
        int degraded = (int) results.stream().filter(RegionResult::isDegraded).count();
        int unreachable = total - reachable;

        List<Double> validMedians = results.stream().filter(RegionResult::reachable).map(RegionResult::medianMs).sorted().toList();
        Double fastest = validMedians.isEmpty() ? null : validMedians.get(0);
        String fastestId = null;
        Double slowest = validMedians.isEmpty() ? null : validMedians.get(validMedians.size() - 1);
        String slowestId = null;
        for (RegionResult r : results) {
            if (fastest != null && fastest == r.medianMs()) fastestId = r.region().id();
            if (slowest != null && slowest == r.medianMs()) slowestId = r.region().id();
        }

        Double medianOfMedians = validMedians.isEmpty() ? null : Stats.percentile(validMedians, 50.0);
        List<String> alerts = CorrelationDetector.detect(results);

        RunReport report = new RunReport(
            "2.0",
            Instant.now(),
            "2.0.0",
            effectiveMode,
            effectiveSamples,
            effectiveTimeoutMs,
            effectiveConcurrency,
            results,
            total,
            reachable,
            degraded,
            unreachable,
            fastest,
            fastestId,
            slowest,
            slowestId,
            medianOfMedians,
            alerts,
            listRes.sourceName(),
            listRes.sourceAge(),
            durationMs,
            effectiveLat,
            effectiveLon
        );

        if (!noHistory && historyStore != null) {
            historyStore.appendRun(report);
        }

        // Machine-readable outputs
        if (jsonOutput) {
            JsonOut.render(report, System.out, false);
            return (unreachable == total) ? ExitCodes.ALL_DOWN_OR_CONTROL_FAIL : (degraded > 0 || unreachable > 0 ? ExitCodes.DEGRADED_OR_DOWN : ExitCodes.SUCCESS);
        }
        if (ndjsonOutput) {
            JsonOut.render(report, System.out, true);
            return (unreachable == total) ? ExitCodes.ALL_DOWN_OR_CONTROL_FAIL : (degraded > 0 || unreachable > 0 ? ExitCodes.DEGRADED_OR_DOWN : ExitCodes.SUCCESS);
        }
        if (csvOutput) {
            CsvOut.render(report, System.out);
            return (unreachable == total) ? ExitCodes.ALL_DOWN_OR_CONTROL_FAIL : (degraded > 0 || unreachable > 0 ? ExitCodes.DEGRADED_OR_DOWN : ExitCodes.SUCCESS);
        }

        // HTML report
        if (reportHtmlFile != null) {
            String html = HtmlReport.generate(report);
            Files.writeString(Path.of(reportHtmlFile), html);
            System.out.println("Generated HTML report written to " + reportHtmlFile);
            return ExitCodes.SUCCESS;
        }

        // TUI mode
        if (tuiMode) {
            org.jline.terminal.Terminal t = TerminalHelper.getTerminal();
            if (t != null) {
                WorldMap wm = new WorldMap(ansi, glyphs, effectiveAscii);
                TuiApp app = new TuiApp(t, ansi, glyphs, wm, report, () -> {
                    try {
                        runProbes(true, false, null);
                    } catch (Exception ignored) {}
                });
                app.run();
                return ExitCodes.SUCCESS;
            } else {
                System.err.println("Notice: Falling back to standard terminal output.");
            }
        }

        // Map mode
        if (mapMode) {
            WorldMap wm = new WorldMap(ansi, glyphs, effectiveAscii);
            wm.render(results, effectiveLat, effectiveLon, TerminalHelper.getWidth(), TerminalHelper.getHeight(), System.out);
            return ExitCodes.SUCCESS;
        }

        // Default Terminal Report
        ReportRenderer renderer = new ReportRenderer(ansi, glyphs, System.out);
        renderer.render(report, TerminalHelper.getWidth(), verbose, compact);

        if (unreachable == total) {
            return ExitCodes.ALL_DOWN_OR_CONTROL_FAIL;
        }
        if (unreachable > 0 || degraded > 0) {
            return ExitCodes.DEGRADED_OR_DOWN;
        }
        return ExitCodes.SUCCESS;
    }

    private static Double parseEnvDouble(String env) {
        String val = System.getenv(env);
        if (val != null && !val.isBlank()) {
            try {
                return Double.parseDouble(val.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private List<RegionGroup> filterGroups(List<RegionGroup> groups) {
        List<RegionGroup> filtered = new ArrayList<>();
        for (RegionGroup g : groups) {
            if (regions != null && !regions.isEmpty()) {
                boolean match = regions.stream().anyMatch(r ->
                    r.equalsIgnoreCase(g.region().id()) ||
                    r.equalsIgnoreCase(g.region().continent()) ||
                    r.equalsIgnoreCase(g.region().country()) ||
                    r.equalsIgnoreCase(g.region().city())
                );
                if (!match) continue;
            }

            if (excludes != null && !excludes.isEmpty()) {
                boolean ex = excludes.stream().anyMatch(e ->
                    e.equalsIgnoreCase(g.region().id()) ||
                    e.equalsIgnoreCase(g.region().continent()) ||
                    e.equalsIgnoreCase(g.region().country()) ||
                    e.equalsIgnoreCase(g.region().city())
                );
                if (ex) continue;
            }

            if (provider != null && !provider.isBlank()) {
                List<Target> pTargets = g.targets().stream()
                    .filter(t -> provider.equalsIgnoreCase(t.provider()))
                    .toList();
                if (pTargets.isEmpty()) continue;
                filtered.add(new RegionGroup(g.region(), pTargets));
                continue;
            }

            filtered.add(g);
        }
        return filtered;
    }

    private List<RegionResult> sortResults(List<RegionResult> results, String sort) {
        List<RegionResult> list = new ArrayList<>(results);
        switch (sort.toLowerCase()) {
            case "latency" -> list.sort(Comparator.comparingDouble(r -> r.reachable() ? r.medianMs() : Double.MAX_VALUE));
            case "delta" -> list.sort(Comparator.comparingDouble(r -> r.baselineDeltaMs() != null ? r.baselineDeltaMs() : 0.0));
            case "efficiency" -> list.sort((a, b) -> Double.compare(b.efficiencyPercent() != null ? b.efficiencyPercent() : 0.0, a.efficiencyPercent() != null ? a.efficiencyPercent() : 0.0));
            case "name" -> list.sort(Comparator.comparing(r -> r.region().city()));
        }
        return list;
    }

    @Command(name = "run", description = "Probe and render report (default command)", mixinStandardHelpOptions = true)
    public static class RunSubcommand implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NardolCommand parent;

        @Override
        public Integer call() throws Exception {
            return parent.runProbes(false, false, null);
        }
    }

    @Command(name = "tui", description = "Live full-screen dashboard", mixinStandardHelpOptions = true)
    public static class TuiSubcommand implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NardolCommand parent;

        @Override
        public Integer call() throws Exception {
            return parent.runProbes(true, false, null);
        }
    }

    @Command(name = "map", description = "Terminal world map of reachability and latencies", mixinStandardHelpOptions = true)
    public static class MapSubcommand implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NardolCommand parent;

        @Override
        public Integer call() throws Exception {
            return parent.runProbes(false, true, null);
        }
    }

    @Command(name = "report", description = "Generate a self-contained offline HTML report", mixinStandardHelpOptions = true)
    public static class ReportSubcommand implements Callable<Integer> {
        @picocli.CommandLine.ParentCommand
        NardolCommand parent;

        @Option(names = {"--from-history"}, description = "Reuse results from most recent run in history")
        boolean fromHistory;

        @Parameters(index = "0", description = "Output HTML file path")
        private File file;

        @Override
        public Integer call() throws Exception {
            return parent.runProbes(false, false, file.getAbsolutePath());
        }
    }
}
