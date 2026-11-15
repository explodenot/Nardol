package com.explodenot.nardol.analysis;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class AnalysisTest {

    @Test
    void testStatsSummary() {
        List<Double> samples = List.of(20.0, 10.0, 30.0, 15.0, 25.0);
        Stats.Summary summary = Stats.calculate(samples, 5);

        assertThat(summary.min()).isEqualTo(10.0);
        assertThat(summary.median()).isEqualTo(20.0);
        assertThat(summary.max()).isEqualTo(30.0);
        assertThat(summary.lossPercent()).isEqualTo(0.0);
        assertThat(summary.jitter()).isCloseTo(7.91, offset(0.05));
    }

    @Test
    void testStatsLoss() {
        List<Double> samples = List.of(20.0);
        Stats.Summary summary = Stats.calculate(samples, 4);
        assertThat(summary.lossPercent()).isEqualTo(75.0);
    }

    @Test
    void testGeoDistanceKnownPair() {
        // New York (40.7128, -74.0060) to London (51.5074, -0.1278) is ~5570 km
        double distance = Geo.haversineKm(40.7128, -74.0060, 51.5074, -0.1278);
        assertThat(distance).isCloseTo(5570.0, offset(50.0));
    }

    @Test
    void testEfficiencyFloorAndAnycastDetection() {
        // Distance 5000 km -> floor RTT = 2 * 5000 / 200 = 50 ms
        Efficiency.Result normal = Efficiency.calculate(5000.0, 80.0);
        assertThat(normal.physicsFloorMs()).isEqualTo(50.0);
        assertThat(normal.efficiencyPercent()).isCloseTo(62.5, offset(0.1));
        assertThat(normal.likelyAnycast()).isFalse();
        assertThat(normal.grade()).isEqualTo(Efficiency.Grade.EXCELLENT);

        // Impossible speed: 5000 km distance but answered in 10 ms
        Efficiency.Result anycast = Efficiency.calculate(5000.0, 10.0);
        assertThat(anycast.likelyAnycast()).isTrue();
        assertThat(anycast.grade()).isEqualTo(Efficiency.Grade.LIKELY_ANYCAST);
    }

    @Test
    void testBaselineComparison() {
        List<Baseline.HistoryRecord> hist = List.of(
            new Baseline.HistoryRecord("host1", 100.0),
            new Baseline.HistoryRecord("host1", 102.0),
            new Baseline.HistoryRecord("host1", 98.0)
        );

        Baseline.Comparison comp = Baseline.compare("host2", 150.0, hist);
        assertThat(comp.baselineMedianMs()).isEqualTo(100.0);
        assertThat(comp.deltaMs()).isEqualTo(50.0);
        assertThat(comp.deltaPercent()).isEqualTo(50.0);
        assertThat(comp.candidateSwitched()).isTrue();
    }
}
