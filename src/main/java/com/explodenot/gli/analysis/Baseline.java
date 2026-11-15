package com.explodenot.gli.analysis;

import java.util.Collections;
import java.util.List;

public final class Baseline {
    private Baseline() {}

    public record Comparison(
        Double baselineMedianMs,
        Double deltaMs,
        Double deltaPercent,
        boolean candidateSwitched,
        List<Double> recentMedians
    ) {}

    public static Comparison compare(
        String currentHost,
        double currentMedianMs,
        List<HistoryRecord> historyForRegion
    ) {
        if (historyForRegion == null || historyForRegion.isEmpty()) {
            return new Comparison(null, null, null, false, Collections.emptyList());
        }

        List<Double> medians = historyForRegion.stream()
            .map(HistoryRecord::medianMs)
            .filter(m -> m != null && m > 0)
            .toList();

        if (medians.isEmpty()) {
            return new Comparison(null, null, null, false, Collections.emptyList());
        }

        double baseMedian = Stats.percentile(medians.stream().sorted().toList(), 50.0);
        baseMedian = Stats.round(baseMedian);

        double deltaMs = Stats.round(currentMedianMs - baseMedian);
        double deltaPct = baseMedian > 0.0 ? Stats.round(((currentMedianMs - baseMedian) / baseMedian) * 100.0) : 0.0;

        String lastHost = historyForRegion.get(historyForRegion.size() - 1).winningHost();
        boolean candidateSwitched = currentHost != null && lastHost != null && !currentHost.equalsIgnoreCase(lastHost);

        // Keep last 10 medians for sparklines
        List<Double> sparklineMedians = medians.size() > 10 ? medians.subList(medians.size() - 10, medians.size()) : medians;

        return new Comparison(baseMedian, deltaMs, deltaPct, candidateSwitched, sparklineMedians);
    }

    public record HistoryRecord(
        String winningHost,
        Double medianMs
    ) {}
}
