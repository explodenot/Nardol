package com.explodenot.nardol.model;

public enum ProbeMode {
    TCP,
    TLS,
    HTTP;

    public static ProbeMode fromString(String s) {
        if (s == null) return TCP;
        return switch (s.trim().toLowerCase()) {
            case "tcp" -> TCP;
            case "tls" -> TLS;
            case "http" -> HTTP;
            default -> throw new IllegalArgumentException("Unknown probe mode: " + s);
        };
    }
}
