package com.explodenot.nardol.render;

import com.explodenot.nardol.model.RegionResult;
import com.explodenot.nardol.model.RunReport;

import java.io.PrintStream;
import java.util.*;

public final class ReportRenderer {
    private final Ansi ansi;
    private final Glyphs glyphs;
    private final PrintStream out;

    public ReportRenderer(Ansi ansi, Glyphs glyphs, PrintStream out) {
        this.ansi = ansi;
        this.glyphs = glyphs;
        this.out = out;
    }

    public void render(RunReport report, int termWidth, boolean verbose, boolean compact) {
        if (report.results().isEmpty()) {
            out.println(ansi.color(Rgb.YELLOW, "No regions probed."));
            return;
        }

        // Group results by continent
        Map<String, List<RegionResult>> byContinent = new LinkedHashMap<>();
        for (RegionResult r : report.results()) {
            byContinent.computeIfAbsent(r.region().continent(), k -> new ArrayList<>()).add(r);
        }

        out.println();
        // Render continent groups
        for (Map.Entry<String, List<RegionResult>> entry : byContinent.entrySet()) {
            String continent = entry.getKey();
            List<RegionResult> list = entry.getValue();

            // Continent Header
            out.println(ansi.bold() + ansi.color(Rgb.CYAN, "─── " + continent + " (" + list.size() + ") ───") + ansi.reset());

            for (RegionResult r : list) {
                renderRegionRow(r, termWidth, verbose, compact);
            }
            out.println();
        }

        // Summary Box
        renderSummary(report, termWidth);
    }

    private void renderRegionRow(RegionResult r, int termWidth, boolean verbose, boolean compact) {
        String statusGlyph;
        Rgb statusColor;
        if (!r.reachable()) {
            statusGlyph = glyphs.failed();
            statusColor = Rgb.RED;
        } else if (r.isDegraded()) {
            statusGlyph = glyphs.degraded();
            statusColor = Rgb.YELLOW;
        } else {
            statusGlyph = glyphs.ok();
            statusColor = Rgb.GREEN;
        }

        String location = r.region().city() + ", " + r.region().country();
        String targetInfo = "";
        if (r.winningTarget() != null) {
            targetInfo = r.winningTarget().provider() + " (" + r.winningTarget().host() + ")";
        } else if (r.primaryFailureMessage() != null) {
            targetInfo = r.primaryFailureMessage();
        }

        StringBuilder row = new StringBuilder();
        row.append(ansi.color(statusColor, TextWidth.padRight(statusGlyph, 3)));
        row.append(ansi.bold()).append(TextWidth.padRight(location, 22)).append(ansi.reset());

        if (r.reachable()) {
            String med = String.format("%6.1f ms", r.medianMs());
            row.append(ansi.color(Rgb.forLatency(r.medianMs()), med)).append(" ");

            // Latency Bar (e.g. 10 chars max)
            int barLen = (int) Math.clamp(Math.round(r.medianMs() / 25.0), 1, 12);
            String bar = "█".repeat(barLen);
            row.append(ansi.color(Rgb.forLatency(r.medianMs()), TextWidth.padRight(bar, 13)));

            // Efficiency
            if (r.efficiencyPercent() != null) {
                if (r.likelyAnycast()) {
                    row.append(ansi.color(Rgb.ORANGE, " [anycast?]"));
                } else {
                    String effStr = String.format(" %3.0f%% eff", r.efficiencyPercent());
                    row.append(ansi.color(r.efficiencyPercent() >= 25 ? Rgb.GREEN : Rgb.YELLOW, effStr));
                }
            } else {
                row.append("          ");
            }

            // Delta
            if (r.baselineDeltaMs() != null) {
                String arrow = (r.baselineDeltaMs() > 0) ? glyphs.upArrow() : glyphs.downArrow();
                Rgb deltaCol = (r.baselineDeltaMs() > 20) ? Rgb.RED : (r.baselineDeltaMs() < -20 ? Rgb.GREEN : Rgb.GRAY);
                String deltaStr = String.format(" %s%+5.1fms", arrow, r.baselineDeltaMs());
                row.append(ansi.color(deltaCol, deltaStr));
            } else {
                row.append("         ");
            }

            // Sparkline
            if (!r.recentHistoryMedians().isEmpty()) {
                String spark = Sparkline.render(r.recentHistoryMedians(), glyphs);
                row.append(" ").append(ansi.color(Rgb.CYAN, spark));
            }

            if (verbose) {
                String p95 = String.format(" p95:%4.0fms", r.p95Ms());
                String jitter = String.format(" jit:%3.0fms", r.jitterMs());
                String loss = String.format(" loss:%2.0f%%", r.lossPercent());
                row.append(ansi.dim()).append(p95).append(jitter).append(loss).append(ansi.reset());

                // Protocol & Cipher if TLS mode or protocol reported
                if (r.samples() != null && !r.samples().isEmpty()) {
                    var outcome = r.samples().get(0).outcome();
                    if (outcome instanceof com.explodenot.nardol.model.ProbeOutcome.Success succ) {
                        if (succ.protocol() != null || succ.cipher() != null) {
                            String tlsInfo = " [" + (succ.protocol() != null ? succ.protocol() : "") +
                                             (succ.cipher() != null ? "/" + succ.cipher() : "") + "]";
                            row.append(ansi.color(Rgb.CYAN, tlsInfo));
                        }
                    }
                }

                row.append(" ").append(ansi.dim()).append(targetInfo).append(ansi.reset());
            } else if (!compact) {
                row.append(" ").append(ansi.dim()).append(TextWidth.truncate(targetInfo, 25)).append(ansi.reset());
            }
        } else {
            row.append(ansi.color(Rgb.RED, "     DOWN   "));
            row.append(ansi.dim()).append(TextWidth.truncate(targetInfo, 35)).append(ansi.reset());
        }

        out.println(TextWidth.truncate(row.toString(), termWidth));
    }

    private void renderSummary(RunReport rep, int termWidth) {
        out.println(ansi.bold() + "╭──────────────────────────────── Summary ────────────────────────────────╮" + ansi.reset());
        String statLine = String.format(
            "│ Reachability: %s reachable · %s degraded · %s down",
            ansi.color(Rgb.GREEN, rep.reachableRegions() + "/" + rep.totalRegions()),
            ansi.color(Rgb.YELLOW, String.valueOf(rep.degradedRegions())),
            ansi.color(Rgb.RED, String.valueOf(rep.unreachableRegions()))
        );
        out.println(statLine);

        if (rep.fastestMedianMs() != null) {
            out.printf(
                "│ Fastest: %s (%.1f ms)   Slowest: %s (%.1f ms)   Median of Medians: %.1f ms%n",
                rep.fastestRegionId(), rep.fastestMedianMs(),
                rep.slowestRegionId(), rep.slowestMedianMs(),
                rep.medianOfMediansMs() != null ? rep.medianOfMediansMs() : 0.0
            );
        }

        out.printf(
            "│ List: %s (%s) · Total probe time: %d ms%n",
            rep.listSource(), rep.listAge(), rep.durationMs()
        );

        if (!rep.correlatedDegradationAlerts().isEmpty()) {
            out.println("│ " + ansi.color(Rgb.ORANGE, "Correlated Degradation Warnings:"));
            for (String alert : rep.correlatedDegradationAlerts()) {
                out.println("│   " + ansi.color(Rgb.YELLOW, glyphs.warning() + " " + alert));
            }
        }

        out.println(ansi.bold() + "╰─────────────────────────────────────────────────────────────────────────╯" + ansi.reset());
    }
}
