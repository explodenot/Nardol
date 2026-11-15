package com.explodenot.nardol.model;

import java.util.Collections;
import java.util.List;

public record RegionGroup(
    Region region,
    List<Target> targets
) {
    public RegionGroup {
        targets = List.copyOf(targets);
    }

    public Target primary() {
        return targets.stream()
            .filter(t -> t.tier() == Tier.PRIMARY)
            .findFirst()
            .orElseGet(() -> targets.isEmpty() ? null : targets.get(0));
    }

    public List<Target> fallbacks() {
        return targets.stream()
            .filter(t -> t.tier() == Tier.FALLBACK)
            .toList();
    }
}
