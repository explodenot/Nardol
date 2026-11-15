package com.explodenot.nardol.render;

import com.explodenot.nardol.model.RegionResult;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.zip.InflaterInputStream;

public final class WorldMap {
    private static final int MASK_WIDTH = 360;
    private static final int MASK_HEIGHT = 180;
    private static final boolean[][] LAND_GRID = new boolean[MASK_HEIGHT][MASK_WIDTH];

    static {
        try (InputStream in = WorldMap.class.getResourceAsStream("/world-mask.bin")) {
            if (in != null) {
                try (InflaterInputStream iis = new InflaterInputStream(in)) {
                    byte[] decompressed = iis.readAllBytes();
                    for (int y = 0; y < MASK_HEIGHT; y++) {
                        for (int x = 0; x < MASK_WIDTH; x++) {
                            int byteIdx = y * ((MASK_WIDTH + 7) / 8) + (x / 8);
                            if (byteIdx < decompressed.length) {
                                int bit = (decompressed[byteIdx] >> (7 - (x % 8))) & 1;
                                LAND_GRID[y][x] = (bit == 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private final Ansi ansi;
    private final Glyphs glyphs;
    private final boolean ascii;

    public WorldMap(Ansi ansi, Glyphs glyphs, boolean ascii) {
        this.ansi = ansi;
        this.glyphs = glyphs;
        this.ascii = ascii;
    }

    public void render(List<RegionResult> results, Double userLat, Double userLon, int termWidth, int termHeight, PrintStream out) {
        int width = Math.min(Math.max(termWidth - 4, 40), 120);
        int height = Math.min(Math.max(termHeight - 8, 15), 40);

        // Precompute marker positions
        Map<String, RegionResult> markerMap = new HashMap<>();
        for (RegionResult r : results) {
            int mx = lonToX(r.region().lon(), width);
            int my = latToY(r.region().lat(), height);
            markerMap.put(mx + "," + my, r);
        }

        String userCoord = null;
        if (userLat != null && userLon != null) {
            userCoord = lonToX(userLon, width) + "," + latToY(userLat, height);
        }

        out.println(ansi.bold() + "╭" + "─".repeat(width) + "╮" + ansi.reset());

        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder("│");
            for (int x = 0; x < width; x++) {
                String coord = x + "," + y;
                if (userCoord != null && userCoord.equals(coord)) {
                    line.append(ansi.color(Rgb.CYAN, ansi.bold() + "@" + ansi.reset()));
                    continue;
                }

                RegionResult r = markerMap.get(coord);
                if (r != null) {
                    Rgb col = !r.reachable() ? Rgb.RED : (r.isDegraded() ? Rgb.YELLOW : Rgb.GREEN);
                    String m = !r.reachable() ? glyphs.failed() : (r.isDegraded() ? glyphs.degraded() : glyphs.bullet());
                    line.append(ansi.color(col, m));
                    continue;
                }

                // Sample land mask
                boolean isLand = sampleLand(x, y, width, height);
                if (isLand) {
                    if (ascii) {
                        line.append(ansi.dim() + "." + ansi.reset());
                    } else {
                        line.append(ansi.dim() + "·" + ansi.reset());
                    }
                } else {
                    line.append(" ");
                }
            }
            line.append("│");
            out.println(line);
        }

        out.println(ansi.bold() + "╰" + "─".repeat(width) + "╯" + ansi.reset());
        out.println("  Legend: " +
            ansi.color(Rgb.GREEN, glyphs.bullet() + " Normal  ") +
            ansi.color(Rgb.YELLOW, glyphs.degraded() + " Degraded  ") +
            ansi.color(Rgb.RED, glyphs.failed() + " Down  ") +
            (userCoord != null ? ansi.color(Rgb.CYAN, "@ You") : "")
        );
    }

    private boolean sampleLand(int x, int y, int width, int height) {
        int gx = (int) Math.round((double) x / width * (MASK_WIDTH - 1));
        int gy = (int) Math.round((double) y / height * (MASK_HEIGHT - 1));
        gx = Math.clamp(gx, 0, MASK_WIDTH - 1);
        gy = Math.clamp(gy, 0, MASK_HEIGHT - 1);
        return LAND_GRID[gy][gx];
    }

    private int lonToX(double lon, int width) {
        // -180..180 -> 0..width-1
        double norm = (lon + 180.0) / 360.0;
        return Math.clamp((int) Math.round(norm * (width - 1)), 0, width - 1);
    }

    private int latToY(double lat, int height) {
        // 90..-90 -> 0..height-1
        double norm = (90.0 - lat) / 180.0;
        return Math.clamp((int) Math.round(norm * (height - 1)), 0, height - 1);
    }
}
