package com.explodenot.gli.analysis;

import com.explodenot.gli.model.RegionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CorrelationDetector {
    private CorrelationDetector() {}

    private static final double CONTINENT_DEGRADED_THRESHOLD = 0.40; // 40%

    public static List<String> detect(List<RegionResult> results) {
        List<String> alerts = new ArrayList<>();
        if (results == null || results.isEmpty()) return alerts;

        Map<String, List<RegionResult>> byContinent = new HashMap<>();
        for (RegionResult r : results) {
            byContinent.computeIfAbsent(r.region().continent(), k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<RegionResult>> entry : byContinent.entrySet()) {
            String continent = entry.getKey();
            List<RegionResult> continentResults = entry.getValue();
            if (continentResults.size() < 2) continue;

            long degradedCount = continentResults.stream()
                .filter(RegionResult::isDegraded)
                .count();

            double ratio = (double) degradedCount / continentResults.size();
            if (ratio >= CONTINENT_DEGRADED_THRESHOLD) {
                alerts.add(String.format(
                    "Heuristic Alert: %s-bound paths degraded (%d/%d regions degraded vs baseline): " +
                    "possible shared upstream routing or submarine cable issue (cannot prove causation).",
                    continent, degradedCount, continentResults.size()
                ));
            }
        }

        return alerts;
    }
}
