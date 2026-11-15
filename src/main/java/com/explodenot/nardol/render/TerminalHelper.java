package com.explodenot.gli.render;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public final class TerminalHelper {
    private static Terminal jlineTerminal;
    private static boolean jlineFailed = false;

    static {
        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
    }

    private TerminalHelper() {}

    public static synchronized Terminal getTerminal() {
        if (jlineTerminal != null) return jlineTerminal;
        if (jlineFailed) return null;

        try {
            jlineTerminal = TerminalBuilder.builder()
                .jansi(false)
                .jni(true)
                .system(true)
                .dumb(true)
                .build();
            return jlineTerminal;
        } catch (Throwable t) {
            try {
                jlineTerminal = TerminalBuilder.builder()
                    .system(true)
                    .dumb(true)
                    .build();
                return jlineTerminal;
            } catch (Throwable t2) {
                jlineFailed = true;
                System.err.println("Notice: JLine terminal provider unavailable (" + t2.getMessage() + "), falling back to plain terminal mode.");
                return null;
            }
        }
    }

    public static int getWidth() {
        Terminal t = getTerminal();
        if (t != null && t.getWidth() > 0) {
            return t.getWidth();
        }
        String cols = System.getenv("COLUMNS");
        if (cols != null) {
            try {
                return Integer.parseInt(cols.trim());
            } catch (Exception ignored) {}
        }
        return 100; // default standard width
    }

    public static int getHeight() {
        Terminal t = getTerminal();
        if (t != null && t.getHeight() > 0) {
            return t.getHeight();
        }
        String lines = System.getenv("LINES");
        if (lines != null) {
            try {
                return Integer.parseInt(lines.trim());
            } catch (Exception ignored) {}
        }
        return 30;
    }
}
