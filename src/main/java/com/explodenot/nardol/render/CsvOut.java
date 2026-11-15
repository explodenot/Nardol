package com.explodenot.gli.render;

import com.explodenot.gli.model.RegionResult;
import com.explodenot.gli.model.RunReport;

import java.io.PrintStream;

public final class CsvOut {
    private CsvOut() {}

    public static void render(RunReport report, PrintStream out) {
        out.println("region_id,continent,country,city,status,median_ms,p95_ms,loss_pct,efficiency_pct,winning_host");
        for (RegionResult r : report.results()) {
            String status = !r.reachable() ? "DOWN" : (r.isDegraded() ? "DEGRADED" : "OK");
            String host = r.winningTarget() != null ? r.winningTarget().host() : "";
            out.printf(
                "%s,%s,%s,%s,%s,%.2f,%.2f,%.1f,%s,%s%n",
                escapeCsv(r.region().id()),
                escapeCsv(r.region().continent()),
                escapeCsv(r.region().country()),
                escapeCsv(r.region().city()),
                status,
                r.medianMs(),
                r.p95Ms(),
                r.lossPercent(),
                r.efficiencyPercent() != null ? String.format("%.1f", r.efficiencyPercent()) : "",
                escapeCsv(host)
            );
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
