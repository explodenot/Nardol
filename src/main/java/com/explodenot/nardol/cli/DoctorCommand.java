package com.explodenot.nardol.cli;

import com.explodenot.nardol.probe.ControlChecks;
import com.explodenot.nardol.render.Ansi;
import com.explodenot.nardol.render.ColorDepth;
import com.explodenot.nardol.render.Rgb;
import com.explodenot.nardol.render.TerminalHelper;
import com.explodenot.nardol.util.Platform;
import org.jline.terminal.Terminal;
import picocli.CommandLine.Command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "doctor", description = "Diagnose the local environment, network, and terminal", mixinStandardHelpOptions = true)
public class DoctorCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        Ansi ansi = new Ansi(ColorDepth.detect(null, false));
        System.out.println(ansi.bold() + "=== Nardol Environment Doctor ===" + ansi.reset());

        // Java runtime
        System.out.println("Java Runtime: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");

        // OS
        System.out.println("Operating System: " + Platform.osName() + " (" + System.getProperty("os.arch") + ")");

        // Cache dir
        Path cacheDir = Platform.cacheDir();
        boolean cacheWritable = false;
        try {
            Files.createDirectories(cacheDir);
            Path testFile = cacheDir.resolve(".test-write");
            Files.writeString(testFile, "ok");
            Files.deleteIfExists(testFile);
            cacheWritable = true;
        } catch (Exception ignored) {}
        System.out.println("Cache Directory: " + cacheDir + (cacheWritable ? " (writable)" : " [NOT WRITABLE]"));

        // Terminal
        Terminal t = TerminalHelper.getTerminal();
        int width = TerminalHelper.getWidth();
        int height = TerminalHelper.getHeight();
        System.out.printf("Terminal: %s (%dx%d)%n", (t != null ? t.getType() : "fallback plain"), width, height);
        System.out.println("Color depth detected: " + ColorDepth.detect(null, false));

        // Network controls
        System.out.println("\nChecking network connectivity:");
        ControlChecks.Diagnostic diag = ControlChecks.run();
        System.out.printf("  DNS resolution (one.one.one.one): %s%n", diag.dnsOk() ? ansi.color(Rgb.GREEN, "OK") : ansi.color(Rgb.RED, "FAILED"));
        System.out.printf("  Raw IP reachability (1.1.1.1, 8.8.8.8, Quad9 9.9.9.9, Cisco Umbrella 208.67.222.222 on 53/443, plus IPv6): %s%n", diag.rawIpOk() ? ansi.color(Rgb.GREEN, "OK") : ansi.color(Rgb.RED, "FAILED"));

        if (diag.proxyDetected()) {
            System.out.println(ansi.color(Rgb.YELLOW, "  Proxy warning: " + diag.proxyWarning()));
        } else {
            System.out.println("  Proxies: none detected in environment/JVM");
        }

        if (!diag.passed()) {
            System.out.println(ansi.color(Rgb.RED, "\nLocal network connectivity appears impaired."));
            return ExitCodes.ALL_DOWN_OR_CONTROL_FAIL;
        }

        System.out.println(ansi.color(Rgb.GREEN, "\nAll environment checks passed cleanly!"));
        return ExitCodes.SUCCESS;
    }
}
