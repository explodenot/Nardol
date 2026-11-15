package com.explodenot.nardol.cli;

import com.explodenot.nardol.history.HistoryStore;
import com.explodenot.nardol.render.Ansi;
import com.explodenot.nardol.render.ColorDepth;
import com.explodenot.nardol.render.Rgb;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "history", description = "Summaries, trends, and export of past probe runs", mixinStandardHelpOptions = true)
public class HistoryCommand implements Callable<Integer> {

    @Option(names = {"--export"}, description = "Export format (json or csv)")
    private String exportFormat;

    @Option(names = {"--compare"}, description = "Compare recent runs")
    private boolean compare;

    @Override
    public Integer call() {
        HistoryStore store = new HistoryStore();
        List<HistoryStore.HistoryEntry> entries = store.loadEntries();

        if (entries.isEmpty()) {
            System.err.println("No history entries found.");
            return ExitCodes.SUCCESS;
        }

        if ("json".equalsIgnoreCase(exportFormat)) {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(entries));
            return ExitCodes.SUCCESS;
        } else if ("csv".equalsIgnoreCase(exportFormat)) {
            System.out.println("timestamp,tool_version,region_id,winning_host,median_ms,p95_ms,loss_pct,reachable,address_family");
            for (HistoryStore.HistoryEntry e : entries) {
                System.out.printf("%s,%s,%s,%s,%.2f,%.2f,%.1f,%b,%s%n",
                    e.timestamp(), e.toolVersion(), e.regionId(),
                    e.winningHost() != null ? e.winningHost() : "",
                    e.medianMs(), e.p95Ms(), e.lossPercent(), e.reachable(), e.addressFamily()
                );
            }
            return ExitCodes.SUCCESS;
        }

        Ansi ansi = new Ansi(ColorDepth.detect(null, false));
        System.out.println(ansi.bold() + "=== History Records (" + entries.size() + " total samples) ===" + ansi.reset());
        System.out.printf("%-20s %-16s %-25s %10s %10s%n", "Timestamp", "Region", "Host", "Median", "Status");
        int count = 0;
        for (int i = entries.size() - 1; i >= 0 && count < 25; i--, count++) {
            HistoryStore.HistoryEntry e = entries.get(i);
            String status = e.reachable() ? ansi.color(Rgb.GREEN, "OK") : ansi.color(Rgb.RED, "DOWN");
            System.out.printf("%-20s %-16s %-25s %8.1f ms %s%n",
                e.timestamp().substring(0, Math.min(19, e.timestamp().length())),
                e.regionId(),
                e.winningHost() != null ? e.winningHost() : "none",
                e.medianMs(),
                status
            );
        }
        return ExitCodes.SUCCESS;
    }
}
