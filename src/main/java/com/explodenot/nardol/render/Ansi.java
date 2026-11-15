package com.explodenot.nardol.render;

public final class Ansi {
    private final ColorDepth depth;

    public Ansi(ColorDepth depth) {
        this.depth = depth;
    }

    public String reset() {
        return (depth == ColorDepth.NONE) ? "" : "\u001B[0m";
    }

    public String bold() {
        return (depth == ColorDepth.NONE) ? "" : "\u001B[1m";
    }

    public String dim() {
        return (depth == ColorDepth.NONE) ? "" : "\u001B[2m";
    }

    public String color(Rgb rgb, String text) {
        if (depth == ColorDepth.NONE) {
            return text;
        }
        String seq = switch (depth) {
            case TRUECOLOR -> String.format("\u001B[38;2;%d;%d;%dm", rgb.r(), rgb.g(), rgb.b());
            case ANSI256 -> String.format("\u001B[38;5;%dm", rgb.toAnsi256());
            case ANSI16 -> String.format("\u001B[%dm", rgb.toAnsi16());
            case NONE -> "";
        };
        return seq + text + reset();
    }

    public String colorBg(Rgb rgb, String text) {
        if (depth == ColorDepth.NONE) {
            return text;
        }
        String seq = switch (depth) {
            case TRUECOLOR -> String.format("\u001B[48;2;%d;%d;%dm", rgb.r(), rgb.g(), rgb.b());
            case ANSI256 -> String.format("\u001B[48;5;%dm", rgb.toAnsi256());
            case ANSI16 -> String.format("\u001B[%dm", rgb.toAnsi16() + 10);
            case NONE -> "";
        };
        return seq + text + reset();
    }
}
