package com.explodenot.gli.render;

public record Glyphs(
    String ok,
    String degraded,
    String failed,
    String warning,
    String bullet,
    String upArrow,
    String downArrow,
    String unchanged,
    String spinnerChars,
    String sparklineChars
) {
    public static final Glyphs UNICODE = new Glyphs(
        "✔",
        "▲",
        "✖",
        "⚠",
        "●",
        "↑",
        "↓",
        "→",
        "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏",
        " ▂▃▄▅▆▇█"
    );

    public static final Glyphs ASCII = new Glyphs(
        "[OK]",
        "[SLOW]",
        "[FAIL]",
        "[WARN]",
        "*",
        "^",
        "v",
        "-",
        "|/-\\",
        "_-~="
    );

    public static Glyphs create(boolean ascii) {
        return ascii ? ASCII : UNICODE;
    }
}
