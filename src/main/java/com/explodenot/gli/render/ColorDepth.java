package com.explodenot.gli.render;

public enum ColorDepth {
    TRUECOLOR,
    ANSI256,
    ANSI16,
    NONE;

    public static ColorDepth detect(String explicit, boolean noColorFlag) {
        if (noColorFlag || System.getenv("NO_COLOR") != null) {
            return NONE;
        }

        if (explicit != null && !explicit.isBlank()) {
            return switch (explicit.trim().toLowerCase()) {
                case "truecolor", "24bit" -> TRUECOLOR;
                case "256", "ansi256" -> ANSI256;
                case "16", "ansi16" -> ANSI16;
                case "none" -> NONE;
                default -> TRUECOLOR;
            };
        }

        String colorterm = System.getenv("COLORTERM");
        if (colorterm != null && (colorterm.contains("truecolor") || colorterm.contains("24bit"))) {
            return TRUECOLOR;
        }

        String term = System.getenv("TERM");
        if (term != null && (term.contains("256color") || term.contains("kitty") || term.contains("alacritty"))) {
            return ANSI256;
        }

        return TRUECOLOR; // Modern terminals default to 24-bit RGB
    }
}
