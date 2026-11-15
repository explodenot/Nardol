package com.explodenot.nardol.model;

import java.time.Instant;
import java.util.List;

public record RunReport(
    String schemaVersion,
    Instant timestamp,
    String toolVersion,
    String probeMode,
    int sampleCount,
    double timeoutMs,
    int concurrency,
    List<RegionResult> results,
    int totalRegions,
    int reachableRegions,
    int degradedRegions,
    int unreachableRegions,
    Double fastestMedianMs,
    String fastestRegionId,
    Double slowestMedianMs,
    String slowestRegionId,
    Double medianOfMediansMs,
    List<String> correlatedDegradationAlerts,
    String listSource,
    String listAge,
    long durationMs,
    Double userLat,
    Double userLon
) {}
