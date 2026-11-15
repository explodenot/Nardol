package com.explodenot.gli.model;

public enum Tier {
    PRIMARY,
    FALLBACK;

    public static Tier fromString(String val) {
        if (val == null) return FALLBACK;
        return switch (val.trim().toLowerCase()) {
            case "primary" -> PRIMARY;
            case "fallback" -> FALLBACK;
            default -> throw new IllegalArgumentException("Unknown tier: " + val);
        };
    }
}
