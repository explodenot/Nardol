package com.explodenot.nardol.history;

import com.explodenot.nardol.analysis.Baseline;
import com.explodenot.nardol.model.AddressFamily;
import com.explodenot.nardol.model.RegionResult;
import com.explodenot.nardol.model.RunReport;
import com.explodenot.nardol.util.Platform;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class HistoryStore {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_MAX_RUNS = 100;

    private final Path historyFile;
    private final int maxRuns;

    public record HistoryEntry(
        String timestamp,
        String toolVersion,
        String regionId,
        String winningHost,
        double medianMs,
        double p95Ms,
        double lossPercent,
        boolean reachable,
        String addressFamily
    ) {}

    public HistoryStore() {
        this(Platform.cacheDir().resolve("history.jsonl"), DEFAULT_MAX_RUNS);
    }

    public HistoryStore(Path historyFile, int maxRuns) {
        this.historyFile = historyFile;
        this.maxRuns = maxRuns;
    }

    public synchronized void appendRun(RunReport report) {
        if (report == null || report.results() == null) return;
        try {
            if (historyFile.getParent() != null) {
                Files.createDirectories(historyFile.getParent());
            }

            List<HistoryEntry> currentEntries = loadEntries();

            for (RegionResult r : report.results()) {
                currentEntries.add(new HistoryEntry(
                    report.timestamp().toString(),
                    report.toolVersion(),
                    r.region().id(),
                    r.winningTarget() != null ? r.winningTarget().host() : null,
                    r.medianMs(),
                    r.p95Ms(),
                    r.lossPercent(),
                    r.reachable(),
                    r.winningTarget() != null ? "AUTO" : "UNKNOWN"
                ));
            }

            // Rotate if necessary: approximate runs by unique timestamps
            Set<String> uniqueTimestamps = new LinkedHashSet<>();
            for (HistoryEntry e : currentEntries) {
                uniqueTimestamps.add(e.timestamp());
            }

            if (uniqueTimestamps.size() > maxRuns) {
                List<String> sortedTs = new ArrayList<>(uniqueTimestamps);
                Set<String> allowedTs = new HashSet<>(sortedTs.subList(sortedTs.size() - maxRuns, sortedTs.size()));
                currentEntries.removeIf(e -> !allowedTs.contains(e.timestamp()));
            }

            // Write out cleanly
            try (BufferedWriter writer = Files.newBufferedWriter(historyFile, StandardCharsets.UTF_8)) {
                for (HistoryEntry e : currentEntries) {
                    writer.write(GSON.toJson(e));
                    writer.newLine();
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized List<HistoryEntry> loadEntries() {
        if (!Files.exists(historyFile)) return new ArrayList<>();
        List<HistoryEntry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    HistoryEntry entry = GSON.fromJson(trimmed, HistoryEntry.class);
                    if (entry != null && entry.regionId() != null) {
                        entries.add(entry);
                    }
                } catch (Exception ignored) {
                    // Ignore corrupt line gracefully
                }
            }
        } catch (Exception ignored) {}
        return entries;
    }

    public synchronized Baseline.Comparison compareWithBaseline(String regionId, String currentHost, double currentMedian) {
        List<HistoryEntry> all = loadEntries();
        List<Baseline.HistoryRecord> records = all.stream()
            .filter(e -> regionId.equals(e.regionId()) && e.reachable())
            .map(e -> new Baseline.HistoryRecord(e.winningHost(), e.medianMs()))
            .toList();

        return Baseline.compare(currentHost, currentMedian, records);
    }
}
