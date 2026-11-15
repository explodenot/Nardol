package com.explodenot.nardol.render;

public final class TextWidth {
    private TextWidth() {}

    /**
     * Compute display columns for a string, accounting for ANSI escapes and East Asian wide characters.
     */
    public static int displayWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        int width = 0;
        boolean inAnsi = false;

        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);

            if (cp == 0x1B) { // ESC
                inAnsi = true;
                continue;
            }
            if (inAnsi) {
                if ((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')) {
                    inAnsi = false;
                }
                continue;
            }

            width += charDisplayWidth(cp);
        }
        return width;
    }

    public static int charDisplayWidth(int cp) {
        if (Character.isISOControl(cp)) return 0;
        // East Asian Wide / Fullwidth characters and common emojis
        if ((cp >= 0x1100 && cp <= 0x115F) ||
            (cp >= 0x2E80 && cp <= 0xA4CF && cp != 0x303F) ||
            (cp >= 0xAC00 && cp <= 0xD7A3) ||
            (cp >= 0xF900 && cp <= 0xFAFF) ||
            (cp >= 0xFE10 && cp <= 0xFE19) ||
            (cp >= 0xFE30 && cp <= 0xFE6F) ||
            (cp >= 0xFF00 && cp <= 0xFF60) ||
            (cp >= 0xFFE0 && cp <= 0xFFE6) ||
            (cp >= 0x1F300 && cp <= 0x1F64F) ||
            (cp >= 0x1F900 && cp <= 0x1F9FF)) {
            return 2;
        }
        return 1;
    }

    public static String padRight(String s, int targetWidth) {
        int w = displayWidth(s);
        if (w >= targetWidth) return s;
        return s + " ".repeat(targetWidth - w);
    }

    public static String padLeft(String s, int targetWidth) {
        int w = displayWidth(s);
        if (w >= targetWidth) return s;
        return " ".repeat(targetWidth - w) + s;
    }

    public static String truncate(String s, int maxWidth) {
        if (displayWidth(s) <= maxWidth) return s;
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        boolean inAnsi = false;

        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);

            if (cp == 0x1B) {
                inAnsi = true;
                sb.appendCodePoint(cp);
                continue;
            }
            if (inAnsi) {
                sb.appendCodePoint(cp);
                if ((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')) {
                    inAnsi = false;
                }
                continue;
            }

            int w = charDisplayWidth(cp);
            if (currentWidth + w > maxWidth) break;
            sb.appendCodePoint(cp);
            currentWidth += w;
        }
        return sb.toString();
    }
}
