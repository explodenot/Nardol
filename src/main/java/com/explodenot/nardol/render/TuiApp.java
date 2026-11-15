package com.explodenot.nardol.render;

import com.explodenot.nardol.model.RegionResult;
import com.explodenot.nardol.model.RunReport;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;

public final class TuiApp {
    private final Terminal terminal;
    private final Ansi ansi;
    private final Glyphs glyphs;
    private final WorldMap worldMap;
    private final Runnable rerunAction;

    private List<RegionResult> results;
    private RunReport currentReport;
    private boolean showingMap = false;
    private boolean verbose = false;
    private int scrollOffset = 0;
    private String continentFilter = null; // null = all
    private int sortMode = 0; // 0=name, 1=latency, 2=delta, 3=efficiency

    public TuiApp(
        Terminal terminal,
        Ansi ansi,
        Glyphs glyphs,
        WorldMap worldMap,
        RunReport initialReport,
        Runnable rerunAction
    ) {
        this.terminal = terminal;
        this.ansi = ansi;
        this.glyphs = glyphs;
        this.worldMap = worldMap;
        this.currentReport = initialReport;
        this.results = new ArrayList<>(initialReport.results());
        this.rerunAction = rerunAction;
    }

    public void updateReport(RunReport report) {
        this.currentReport = report;
        this.results = new ArrayList<>(report.results());
        render();
    }

    public void run() {
        if (terminal == null) {
            System.err.println("Cannot run TUI: interactive terminal unavailable.");
            return;
        }

        Attributes origAttributes = terminal.enterRawMode();
        PrintWriter writer = terminal.writer();

        // Switch to alternate screen and hide cursor
        writer.print("\u001B[?1049h\u001B[?25l");
        writer.flush();

        Reader reader = terminal.reader();

        try {
            render();

            boolean running = true;
            while (running) {
                int ch = reader.read();
                if (ch == -1 || ch == 'q' || ch == 'Q' || ch == 3) { // 3 = Ctrl+C
                    running = false;
                } else if (ch == 'm' || ch == 'M') {
                    showingMap = !showingMap;
                    render();
                } else if (ch == 'v' || ch == 'V') {
                    verbose = !verbose;
                    render();
                } else if (ch == 's' || ch == 'S') {
                    sortMode = (sortMode + 1) % 4;
                    render();
                } else if (ch == 'f' || ch == 'F') {
                    cycleFilter();
                    render();
                } else if (ch == 'r' || ch == 'R') {
                    if (rerunAction != null) {
                        rerunAction.run();
                    }
                } else if (ch == 27) { // ESC sequence (arrows)
                    if (reader.ready()) {
                        int next = reader.read();
                        if (next == '[') {
                            int code = reader.read();
                            if (code == 'A') { // Up
                                scrollOffset = Math.max(0, scrollOffset - 1);
                                render();
                            } else if (code == 'B') { // Down
                                scrollOffset++;
                                render();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            // Restore screen and show cursor
            writer.print("\u001B[?25h\u001B[?1049l");
            writer.flush();
            terminal.setAttributes(origAttributes);
        }
    }

    private void cycleFilter() {
        Set<String> continents = new TreeSet<>();
        for (RegionResult r : currentReport.results()) {
            continents.add(r.region().continent());
        }
        List<String> list = new ArrayList<>(continents);
        if (continentFilter == null) {
            continentFilter = list.isEmpty() ? null : list.get(0);
        } else {
            int idx = list.indexOf(continentFilter);
            if (idx == -1 || idx == list.size() - 1) {
                continentFilter = null;
            } else {
                continentFilter = list.get(idx + 1);
            }
        }
    }

    private void render() {
        PrintWriter writer = terminal.writer();
        int width = terminal.getWidth();
        int height = terminal.getHeight();

        // Clear screen and home
        writer.print("\u001B[H\u001B[2J");

        // Header
        String sortName = switch (sortMode) {
            case 1 -> "Latency";
            case 2 -> "Delta";
            case 3 -> "Efficiency";
            default -> "Name";
        };
        String header = String.format(
            "Nardol v%s · Reachable: %d/%d · Filter: %s · Sort: %s · View: %s",
            currentReport.toolVersion(),
            currentReport.reachableRegions(), currentReport.totalRegions(),
            continentFilter != null ? continentFilter : "All",
            sortName,
            showingMap ? "Map" : "Table"
        );
        writer.println(ansi.bold() + ansi.colorBg(Rgb.DARK_GRAY, TextWidth.padRight(" " + header, width)) + ansi.reset());

        if (showingMap) {
            worldMap.render(results, currentReport.userLat(), currentReport.userLon(), width, height - 3, System.out);
        } else {
            renderTable(writer, width, height - 3);
        }

        // Footer
        String footer = " [q] Quit  [r] Re-run  [s] Sort  [f] Filter  [v] Verbose  [m] Toggle Map  [↑/↓] Scroll";
        writer.println(ansi.dim() + TextWidth.padRight(footer, width) + ansi.reset());
        writer.flush();
    }

    private void renderTable(PrintWriter writer, int width, int maxLines) {
        List<RegionResult> displayList = new ArrayList<>(results);
        if (continentFilter != null) {
            displayList.removeIf(r -> !continentFilter.equalsIgnoreCase(r.region().continent()));
        }

        switch (sortMode) {
            case 1 -> displayList.sort(Comparator.comparingDouble(RegionResult::medianMs));
            case 2 -> displayList.sort(Comparator.comparingDouble(r -> r.baselineDeltaMs() != null ? r.baselineDeltaMs() : 0.0));
            case 3 -> displayList.sort((a, b) -> Double.compare(b.efficiencyPercent() != null ? b.efficiencyPercent() : 0.0, a.efficiencyPercent() != null ? a.efficiencyPercent() : 0.0));
            default -> displayList.sort(Comparator.comparing(r -> r.region().city()));
        }

        int visibleLines = Math.max(1, maxLines - 1);
        int start = Math.min(scrollOffset, Math.max(0, displayList.size() - visibleLines));
        int end = Math.min(start + visibleLines, displayList.size());

        for (int i = start; i < end; i++) {
            RegionResult r = displayList.get(i);
            String status = !r.reachable() ? glyphs.failed() : (r.isDegraded() ? glyphs.degraded() : glyphs.ok());
            Rgb col = !r.reachable() ? Rgb.RED : (r.isDegraded() ? Rgb.YELLOW : Rgb.GREEN);

            String city = TextWidth.padRight(r.region().city() + ", " + r.region().country(), 22);
            String med = r.reachable() ? String.format("%6.1f ms", r.medianMs()) : "  DOWN   ";
            String eff = r.efficiencyPercent() != null ? String.format("%3.0f%% eff", r.efficiencyPercent()) : "        ";
            String host = r.winningTarget() != null ? r.winningTarget().host() : (r.primaryFailureMessage() != null ? r.primaryFailureMessage() : "");

            String row = ansi.color(col, status) + " " + city + " " + ansi.color(Rgb.forLatency(r.medianMs()), med) + " " + eff + " " + ansi.dim() + host + ansi.reset();
            writer.println(TextWidth.truncate(row, width));
        }
    }
}
