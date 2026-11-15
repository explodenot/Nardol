package com.explodenot.gli.model;

import java.util.List;

public record RegionResult(
    Region region,
    Target winningTarget,
    List<ProbeSample> samples,
    double minMs,
    double medianMs,
    double p95Ms,
    double maxMs,
    double jitterMs,
    double lossPercent,
    boolean reachable,
    String primaryFailureMessage,
    Double physicsFloorMs,
    Double efficiencyPercent,
    boolean likelyAnycast,
    Double baselineMedianMs,
    Double baselineDeltaMs,
    Double baselineDeltaPercent,
    boolean candidateSwitched,
    List<Double> recentHistoryMedians,
    // Dual-stack fields
    RegionResult ipv4Result,
    RegionResult ipv6Result,
    String dualStackWinner
) {
    public boolean isDegraded() {
        if (!reachable) return false;
        if (baselineDeltaPercent != null && baselineDeltaPercent > 50.0) return true;
        if (efficiencyPercent != null && efficiencyPercent < 25.0 && !likelyAnycast) return true;
        return lossPercent > 0.0 && lossPercent < 100.0;
    }
}
