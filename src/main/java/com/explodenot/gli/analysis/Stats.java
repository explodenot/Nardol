package com.explodenot.gli.analysis;

import java.util.Collections;
import java.util.List;

public final class Stats {
    private Stats() {}

    public record Summary(
        double min,
        double median,
        double p95,
        double max,
        double jitter,
        double lossPercent
    ) {}

    public static Summary calculate(List<Double> latencies, int totalAttempted) {
        if (totalAttempted <= 0) {
            return new Summary(0.0, 0.0, 0.0, 0.0, 0.0, 100.0);
        }
        if (latencies == null || latencies.isEmpty()) {
            return new Summary(0.0, 0.0, 0.0, 0.0, 0.0, 100.0);
        }

        List<Double> sorted = latencies.stream().sorted().toList();
        int n = sorted.size();
        double lossPercent = ((double) (totalAttempted - n) / totalAttempted) * 100.0;

        double min = sorted.get(0);
        double max = sorted.get(n - 1);
        double median = percentile(sorted, 50.0);
        double p95 = percentile(sorted, 95.0);

        // Jitter: standard deviation of samples
        double mean = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double sumSq = 0.0;
        for (double v : sorted) {
            sumSq += (v - mean) * (v - mean);
        }
        double jitter = n > 1 ? Math.sqrt(sumSq / (n - 1)) : 0.0;

        return new Summary(round(min), round(median), round(p95), round(max), round(jitter), round(lossPercent));
    }

    public static double percentile(List<Double> sorted, double pct) {
        if (sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.get(0);
        double rank = (pct / 100.0) * (sorted.size() - 1);
        int low = (int) Math.floor(rank);
        int high = (int) Math.ceil(rank);
        double fraction = rank - low;
        return sorted.get(low) + fraction * (sorted.get(high) - sorted.get(low));
    }

    public static double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
