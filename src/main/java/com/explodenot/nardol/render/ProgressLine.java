package com.explodenot.nardol.render;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProgressLine {
    private final Ansi ansi;
    private final Glyphs glyphs;
    private final int total;
    private final AtomicInteger done = new AtomicInteger(0);
    private final AtomicInteger okCount = new AtomicInteger(0);
    private final AtomicInteger slowCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final long startTime;
    private final boolean isTty;
    private final PrintStream out;
    private Thread animationThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ProgressLine(Ansi ansi, Glyphs glyphs, int total, boolean isTty, PrintStream out) {
        this.ansi = ansi;
        this.glyphs = glyphs;
        this.total = total;
        this.isTty = isTty;
        this.out = out;
        this.startTime = System.currentTimeMillis();
    }

    public void start() {
        if (!isTty) return;
        running.set(true);
        // Hide cursor
        out.print("\u001B[?25l");
        out.flush();

        animationThread = Thread.ofVirtual().start(() -> {
            int frame = 0;
            String spin = glyphs.spinnerChars();
            while (running.get()) {
                char s = spin.charAt(frame % spin.length());
                renderLine(s);
                frame++;
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    public void record(boolean ok, boolean slow) {
        done.incrementAndGet();
        if (!ok) {
            failCount.incrementAndGet();
        } else if (slow) {
            slowCount.incrementAndGet();
        } else {
            okCount.incrementAndGet();
        }
    }

    private synchronized void renderLine(char spinnerChar) {
        if (!isTty) return;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        String line = String.format(
            "\r%s %s %d/%d (%d%%)  %s  %s  %s  [%ds]",
            ansi.color(Rgb.CYAN, String.valueOf(spinnerChar)),
            ansi.bold() + "Probing regions:" + ansi.reset(),
            done.get(), total,
            total > 0 ? (done.get() * 100 / total) : 0,
            ansi.color(Rgb.GREEN, glyphs.ok() + " " + okCount.get()),
            ansi.color(Rgb.YELLOW, glyphs.degraded() + " " + slowCount.get()),
            ansi.color(Rgb.RED, glyphs.failed() + " " + failCount.get()),
            elapsed
        );
        out.print(line);
        out.flush();
    }

    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (animationThread != null) {
                animationThread.interrupt();
            }
            if (isTty) {
                // Clear progress line and show cursor
                out.print("\r\u001B[K\u001B[?25h");
                out.flush();
            }
        }
    }
}
