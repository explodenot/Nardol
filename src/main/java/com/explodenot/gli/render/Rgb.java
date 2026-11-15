package com.explodenot.gli.render;

public record Rgb(int r, int g, int b) {
    public static final Rgb GREEN = new Rgb(46, 204, 113);
    public static final Rgb YELLOW = new Rgb(241, 196, 15);
    public static final Rgb ORANGE = new Rgb(230, 126, 34);
    public static final Rgb RED = new Rgb(231, 76, 60);
    public static final Rgb CYAN = new Rgb(52, 152, 219);
    public static final Rgb GRAY = new Rgb(127, 140, 141);
    public static final Rgb DARK_GRAY = new Rgb(80, 80, 80);
    public static final Rgb WHITE = new Rgb(236, 240, 241);

    public static Rgb interpolate(Rgb c1, Rgb c2, double fraction) {
        fraction = Math.clamp(fraction, 0.0, 1.0);
        int r = (int) Math.round(c1.r + (c2.r - c1.r) * fraction);
        int g = (int) Math.round(c1.g + (c2.g - c1.g) * fraction);
        int b = (int) Math.round(c1.b + (c2.b - c1.b) * fraction);
        return new Rgb(r, g, b);
    }

    public static Rgb forLatency(double latencyMs) {
        if (latencyMs <= 50.0) {
            return interpolate(GREEN, YELLOW, latencyMs / 50.0);
        } else if (latencyMs <= 150.0) {
            return interpolate(YELLOW, ORANGE, (latencyMs - 50.0) / 100.0);
        } else {
            return interpolate(ORANGE, RED, Math.min((latencyMs - 150.0) / 150.0, 1.0));
        }
    }

    public int toAnsi256() {
        // Standard 6x6x6 color cube
        int ri = (int) Math.round(r * 5.0 / 255.0);
        int gi = (int) Math.round(g * 5.0 / 255.0);
        int bi = (int) Math.round(b * 5.0 / 255.0);
        return 16 + (36 * ri) + (6 * gi) + bi;
    }

    public int toAnsi16() {
        if (r > 180 && g < 100 && b < 100) return 31; // red
        if (g > 180 && r < 100) return 32; // green
        if (r > 180 && g > 150 && b < 100) return 33; // yellow
        if (b > 180) return 34; // blue
        if (r > 150 && b > 150) return 35; // magenta
        if (g > 150 && b > 150) return 36; // cyan
        return 37; // white
    }
}
