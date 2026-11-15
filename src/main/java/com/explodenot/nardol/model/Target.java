package com.explodenot.nardol.model;

public record Target(
    String regionId,
    String continent,
    String country,
    String city,
    String provider,
    String host,
    int port,
    double lat,
    double lon,
    Tier tier
) {
    public String key() {
        return host + ":" + port;
    }
}
