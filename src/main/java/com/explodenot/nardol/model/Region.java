package com.explodenot.nardol.model;

import java.util.List;

public record Region(
    String id,
    String continent,
    String country,
    String city,
    double lat,
    double lon
) {
    public static Region fromTarget(Target t) {
        return new Region(t.regionId(), t.continent(), t.country(), t.city(), t.lat(), t.lon());
    }
}
