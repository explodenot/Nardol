package com.explodenot.gli.probe;

import com.explodenot.gli.analysis.Baseline;
import com.explodenot.gli.analysis.Efficiency;
import com.explodenot.gli.analysis.Geo;
import com.explodenot.gli.analysis.Stats;
import com.explodenot.gli.history.HistoryStore;
import com.explodenot.gli.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class ProbeRunner {
    private final Prober prober;
    private final HistoryStore historyStore;
    private final Double userLat;
    private final Double userLon;

    public ProbeRunner(Prober prober, HistoryStore historyStore, Double userLat, Double userLon) {
        this.prober = prober;
        this.historyStore = historyStore;
        this.userLat = userLat;
        this.userLon = userLon;
    }

    public List<RegionResult> run(
        List<RegionGroup> groups,
        int sampleCount,
        Duration timeout,
        int concurrency,
        AddressFamily addressFamily,
        boolean allCandidates,
        boolean dualStack,
        Consumer<RegionResult> progressCallback
    ) throws InterruptedException {
        if (groups == null || groups.isEmpty()) return Collections.emptyList();

        Semaphore globalSemaphore = new Semaphore(concurrency);
        ConcurrentHashMap<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();
        List<RegionResult> results = Collections.synchronizedList(new ArrayList<>());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (RegionGroup group : groups) {
                futures.add(executor.submit(() -> {
                    try {
                        RegionResult res;
                        if (dualStack) {
                            res = probeDualStack(group, sampleCount, timeout, globalSemaphore, hostSemaphores, allCandidates);
                        } else {
                            res = probeGroup(group, sampleCount, timeout, globalSemaphore, hostSemaphores, addressFamily, allCandidates);
                        }
                        results.add(res);
                        if (progressCallback != null) {
                            progressCallback.accept(res);
                        }
                    } catch (Throwable ignored) {}
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException ignored) {}
            }
        }

        // Return ordered identically to original groups
        Map<String, RegionResult> map = new HashMap<>();
        for (RegionResult r : results) {
            map.put(r.region().id(), r);
        }
        List<RegionResult> ordered = new ArrayList<>();
        for (RegionGroup g : groups) {
            RegionResult r = map.get(g.region().id());
            if (r != null) {
                ordered.add(r);
            }
        }
        return ordered;
    }

    private RegionResult probeDualStack(
        RegionGroup group,
        int sampleCount,
        Duration timeout,
        Semaphore globalSemaphore,
        ConcurrentHashMap<String, Semaphore> hostSemaphores,
        boolean allCandidates
    ) {
        RegionResult v4 = probeGroup(group, sampleCount, timeout, globalSemaphore, hostSemaphores, AddressFamily.IPV4, allCandidates);
        RegionResult v6 = probeGroup(group, sampleCount, timeout, globalSemaphore, hostSemaphores, AddressFamily.IPV6, allCandidates);

        String winner = null;
        if (v4.reachable() && v6.reachable()) {
            winner = (v6.medianMs() < v4.medianMs()) ? "IPv6" : "IPv4";
        } else if (v4.reachable()) {
            winner = "IPv4";
        } else if (v6.reachable()) {
            winner = "IPv6";
        }

        RegionResult primaryResult = (v6.reachable() && v6.medianMs() <= v4.medianMs()) ? v6 : v4;
        return new RegionResult(
            primaryResult.region(),
            primaryResult.winningTarget(),
            primaryResult.samples(),
            primaryResult.minMs(),
            primaryResult.medianMs(),
            primaryResult.p95Ms(),
            primaryResult.maxMs(),
            primaryResult.jitterMs(),
            primaryResult.lossPercent(),
            primaryResult.reachable(),
            primaryResult.primaryFailureMessage(),
            primaryResult.physicsFloorMs(),
            primaryResult.efficiencyPercent(),
            primaryResult.likelyAnycast(),
            primaryResult.baselineMedianMs(),
            primaryResult.baselineDeltaMs(),
            primaryResult.baselineDeltaPercent(),
            primaryResult.candidateSwitched(),
            primaryResult.recentHistoryMedians(),
            v4,
            v6,
            winner
        );
    }

    private RegionResult probeGroup(
        RegionGroup group,
        int sampleCount,
        Duration timeout,
        Semaphore globalSemaphore,
        ConcurrentHashMap<String, Semaphore> hostSemaphores,
        AddressFamily addressFamily,
        boolean allCandidates
    ) {
        Target winningTarget = null;
        List<ProbeSample> samples = new ArrayList<>();
        String primaryFailure = null;

        List<Target> candidates = new ArrayList<>();
        if (allCandidates) {
            candidates.addAll(group.targets());
        } else {
            Target primary = group.primary();
            if (primary != null) candidates.add(primary);
            candidates.addAll(group.fallbacks());
        }

        for (Target target : candidates) {
            List<ProbeSample> targetSamples = probeTarget(target, sampleCount, timeout, globalSemaphore, hostSemaphores, addressFamily);
            boolean anySuccess = targetSamples.stream().anyMatch(ProbeSample::successful);

            if (anySuccess) {
                winningTarget = target;
                samples = targetSamples;
                break;
            } else {
                if (target.tier() == Tier.PRIMARY && !targetSamples.isEmpty()) {
                    ProbeOutcome o = targetSamples.get(0).outcome();
                    if (o instanceof ProbeOutcome.Failure f) {
                        primaryFailure = f.message();
                    }
                }
                if (samples.isEmpty()) {
                    samples = targetSamples;
                }
            }
        }

        List<Double> latencies = samples.stream()
            .filter(ProbeSample::successful)
            .map(ProbeSample::latencyMs)
            .toList();

        Stats.Summary summary = Stats.calculate(latencies, sampleCount);
        boolean reachable = winningTarget != null && !latencies.isEmpty();

        Double physicsFloor = null;
        Double efficiencyPct = null;
        boolean likelyAnycast = false;

        if (userLat != null && userLon != null && reachable) {
            double distanceKm = Geo.haversineKm(userLat, userLon, group.region().lat(), group.region().lon());
            Efficiency.Result effRes = Efficiency.calculate(distanceKm, summary.median());
            physicsFloor = effRes.physicsFloorMs();
            efficiencyPct = effRes.efficiencyPercent();
            likelyAnycast = effRes.likelyAnycast();
        }

        Baseline.Comparison baseComp = null;
        if (historyStore != null) {
            baseComp = historyStore.compareWithBaseline(group.region().id(), winningTarget != null ? winningTarget.host() : null, summary.median());
        }

        return new RegionResult(
            group.region(),
            winningTarget,
            samples,
            summary.min(),
            summary.median(),
            summary.p95(),
            summary.max(),
            summary.jitter(),
            summary.lossPercent(),
            reachable,
            primaryFailure,
            physicsFloor,
            efficiencyPct,
            likelyAnycast,
            baseComp != null ? baseComp.baselineMedianMs() : null,
            baseComp != null ? baseComp.deltaMs() : null,
            baseComp != null ? baseComp.deltaPercent() : null,
            baseComp != null && baseComp.candidateSwitched(),
            baseComp != null ? baseComp.recentMedians() : Collections.emptyList(),
            null,
            null,
            null
        );
    }

    private List<ProbeSample> probeTarget(
        Target target,
        int sampleCount,
        Duration timeout,
        Semaphore globalSemaphore,
        ConcurrentHashMap<String, Semaphore> hostSemaphores,
        AddressFamily addressFamily
    ) {
        List<ProbeSample> samples = new ArrayList<>();
        Semaphore hostSemaphore = hostSemaphores.computeIfAbsent(target.host(), h -> new Semaphore(2));

        for (int i = 0; i < sampleCount; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(50); // Small polite spacing between samples
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                globalSemaphore.acquire();
                hostSemaphore.acquire();
                try {
                    ProbeOutcome outcome = prober.probe(target, timeout, addressFamily);
                    boolean ok = (outcome instanceof ProbeOutcome.Success);
                    double lat = ok ? ((ProbeOutcome.Success) outcome).totalMs() : 0.0;
                    samples.add(new ProbeSample(Instant.now(), outcome, lat, ok));
                } finally {
                    hostSemaphore.release();
                    globalSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return samples;
    }
}
