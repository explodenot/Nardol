package com.explodenot.nardol.analysis;

public final class Efficiency {
    private Efficiency() {}

    // Speed of light in fiber optic glass is ~200 km per ms (200,000 km/s)
    public static final double FIBER_SPEED_KM_PER_MS = 200.0;

    public static final double EXCELLENT_THRESHOLD = 50.0;
    public static final double GOOD_THRESHOLD = 25.0;
    public static final double DEGRADED_THRESHOLD = 10.0;

    public record Result(
        double physicsFloorMs,
        double efficiencyPercent,
        boolean likelyAnycast,
        Grade grade
    ) {}

    public enum Grade {
        EXCELLENT,
        GOOD,
        DEGRADED,
        SUSPICIOUS_DETOUR,
        LIKELY_ANYCAST
    }

    public static Result calculate(double distanceKm, double measuredMedianMs) {
        // Round-trip theoretical floor: 2 * distance / 200 km/ms
        double floorMs = (2.0 * distanceKm) / FIBER_SPEED_KM_PER_MS;
        floorMs = Stats.round(floorMs);

        if (measuredMedianMs <= 0.0) {
            return new Result(floorMs, 0.0, false, Grade.SUSPICIOUS_DETOUR);
        }

        // If measured RTT is below theoretical physics floor (with 1ms margin for local rounding), it's likely anycast/CDN
        if (distanceKm > 200.0 && measuredMedianMs < (floorMs * 0.85)) {
            return new Result(floorMs, 0.0, true, Grade.LIKELY_ANYCAST);
        }

        double eff = (floorMs / measuredMedianMs) * 100.0;
        eff = Stats.round(Math.min(eff, 100.0));

        Grade grade;
        if (eff >= EXCELLENT_THRESHOLD) {
            grade = Grade.EXCELLENT;
        } else if (eff >= GOOD_THRESHOLD) {
            grade = Grade.GOOD;
        } else if (eff >= DEGRADED_THRESHOLD) {
            grade = Grade.DEGRADED;
        } else {
            grade = Grade.SUSPICIOUS_DETOUR;
        }

        return new Result(floorMs, eff, false, grade);
    }
}
