package com.explodenot.nardol.render;

import java.util.List;

public final class Sparkline {
    private Sparkline() {}

    public static String render(List<Double> values, Glyphs glyphs) {
        if (values == null || values.isEmpty()) return "";
        String chars = glyphs.sparklineChars();
        if (chars.isEmpty()) return "";

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        StringBuilder sb = new StringBuilder();
        int levels = chars.length();
        for (double v : values) {
            int idx;
            if (max == min) {
                idx = levels / 2;
            } else {
                idx = (int) Math.round(((v - min) / (max - min)) * (levels - 1));
            }
            idx = Math.clamp(idx, 0, levels - 1);
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }
}
