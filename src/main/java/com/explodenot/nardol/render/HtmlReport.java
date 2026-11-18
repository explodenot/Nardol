package com.explodenot.nardol.render;

import com.explodenot.nardol.model.RegionResult;
import com.explodenot.nardol.model.RunReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public final class HtmlReport {
    private HtmlReport() {}

    private static final Gson GSON = JsonOut.GSON;

    public static String generate(RunReport report) {
        String json = GSON.toJson(report);

        String landPath = WorldMap.generateSvgLandPath(1000.0, 500.0);
        StringBuilder rows = new StringBuilder();
        StringBuilder svgMarkers = new StringBuilder();

        for (RegionResult r : report.results()) {
            String status = !r.reachable() ? "Down" : (r.isDegraded() ? "Degraded" : "Reachable");
            String statusClass = status.toLowerCase();

            // SVG coordinates: Equirectangular projection (width=1000, height=500)
            double cx = ((r.region().lon() + 180.0) / 360.0) * 1000.0;
            double cy = ((90.0 - r.region().lat()) / 180.0) * 500.0;
            String markerColor = !r.reachable() ? "#e74c3c" : (r.isDegraded() ? "#f1c40f" : "#2ecc71");

            svgMarkers.append(String.format(
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"6\" fill=\"%s\" stroke=\"#ffffff\" stroke-width=\"1.5\"><title>%s (%s): %.1f ms</title></circle>\n",
                cx, cy, markerColor, escapeHtml(r.region().city()), escapeHtml(r.region().country()), r.medianMs()
            ));

            String targetStr = r.winningTarget() != null ? (r.winningTarget().provider() + " - " + r.winningTarget().host()) : (r.primaryFailureMessage() != null ? r.primaryFailureMessage() : "N/A");

            rows.append("<tr>")
                .append("<td class=\"status-cell\"><span class=\"badge badge-").append(statusClass).append("\">").append(status).append("</span></td>")
                .append("<td>").append(escapeHtml(r.region().city())).append("</td>")
                .append("<td>").append(escapeHtml(r.region().country())).append("</td>")
                .append("<td>").append(escapeHtml(r.region().continent())).append("</td>")
                .append("<td class=\"font-mono\">").append(r.reachable() ? String.format("%.1f ms", r.medianMs()) : "-").append("</td>")
                .append("<td class=\"font-mono\">").append(r.reachable() ? String.format("%.1f ms", r.p95Ms()) : "-").append("</td>")
                .append("<td class=\"font-mono\">").append(r.reachable() ? String.format("%.0f%%", r.lossPercent()) : "-").append("</td>")
                .append("<td class=\"font-mono\">").append(r.efficiencyPercent() != null ? String.format("%.0f%%", r.efficiencyPercent()) : "-").append("</td>")
                .append("<td class=\"font-mono text-muted\">").append(escapeHtml(targetStr)).append("</td>")
                .append("</tr>\n");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Nardol Connectivity Report</title>
              <style>
                :root {
                  --bg: #0f172a;
                  --card: #1e293b;
                  --text: #f8fafc;
                  --text-muted: #94a3b8;
                  --border: #334155;
                  --green: #2ecc71;
                  --yellow: #f1c40f;
                  --red: #e74c3c;
                  --blue: #38bdf8;
                }
                @media (prefers-color-scheme: light) {
                  :root {
                    --bg: #f8fafc;
                    --card: #ffffff;
                    --text: #0f172a;
                    --text-muted: #64748b;
                    --border: #e2e8f0;
                  }
                }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                  background: var(--bg);
                  color: var(--text);
                  margin: 0;
                  padding: 24px;
                }
                .container { max-width: 1200px; margin: 0 auto; }
                header { margin-bottom: 24px; }
                h1 { margin: 0 0 8px 0; font-size: 28px; }
                .subtitle { color: var(--text-muted); font-size: 14px; }
                .stats-grid {
                  display: grid;
                  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                  gap: 16px;
                  margin-bottom: 24px;
                }
                .card {
                  background: var(--card);
                  border: 1px solid var(--border);
                  border-radius: 8px;
                  padding: 16px;
                }
                .card-title { color: var(--text-muted); font-size: 13px; margin-bottom: 6px; }
                .card-val { font-size: 24px; font-weight: 600; }
                .map-card { margin-bottom: 24px; overflow: hidden; }
                svg.world-map { width: 100%; height: auto; max-height: 480px; background: #090d16; border-radius: 6px; }
                table {
                  width: 100%;
                  border-collapse: collapse;
                  text-align: left;
                  font-size: 14px;
                }
                th, td { padding: 10px 14px; border-bottom: 1px solid var(--border); }
                th { color: var(--text-muted); font-weight: 600; }
                .font-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
                .text-muted { color: var(--text-muted); }
                .badge {
                  display: inline-block;
                  padding: 2px 8px;
                  border-radius: 4px;
                  font-size: 12px;
                  font-weight: 600;
                }
                .badge-reachable { background: rgba(46, 204, 113, 0.2); color: var(--green); }
                .badge-degraded { background: rgba(241, 196, 15, 0.2); color: var(--yellow); }
                .badge-down { background: rgba(231, 76, 60, 0.2); color: var(--red); }
                @media print {
                  body { background: white; color: black; }
                  .card { border: 1px solid #ccc; }
                }
              </style>
            </head>
            <body>
              <div class="container">
                <header>
                  <h1>Nardol Reachability Report</h1>
                  <div class="subtitle">Generated: \
""");
        sb.append(escapeHtml(report.timestamp().toString()));
        sb.append(" · Mode: ").append(escapeHtml(report.probeMode()));
        sb.append(" · Tool: ").append(escapeHtml(report.toolVersion()));
        sb.append(" · List: ").append(escapeHtml(report.listSource()));
        sb.append("""
                  </div>
                </header>

                <div class="stats-grid">
                  <div class="card">
                    <div class="card-title">Reachable Regions</div>
                    <div class="card-val" style="color: var(--green)">""");
        sb.append(report.reachableRegions()).append(" / ").append(report.totalRegions());
        sb.append("""
                    </div>
                  </div>
                  <div class="card">
                    <div class="card-title">Degraded Regions</div>
                    <div class="card-val" style="color: var(--yellow)">""");
        sb.append(report.degradedRegions());
        sb.append("""
                    </div>
                  </div>
                  <div class="card">
                    <div class="card-title">Unreachable Regions</div>
                    <div class="card-val" style="color: var(--red)">""");
        sb.append(report.unreachableRegions());
        sb.append("""
                    </div>
                  </div>
                  <div class="card">
                    <div class="card-title">Median of Medians</div>
                    <div class="card-val">""");
        sb.append(String.format("%.1f ms", report.medianOfMediansMs() != null ? report.medianOfMediansMs() : 0.0));
        sb.append("""
                    </div>
                  </div>
                </div>

                <div class="card map-card">
                  <div class="card-title" style="margin-bottom: 12px;">Global Map View</div>
                  <svg class="world-map" viewBox="0 0 1000 500" xmlns="http://www.w3.org/2000/svg">
                    <rect width="1000" height="500" fill="#0d1117" />
                    <!-- World Land Mask -->
                    <path d=\"""" + landPath + """
" fill="#1c2536" />
                    <!-- Equirectangular grid lines -->
                    <line x1="0" y1="250" x2="1000" y2="250" stroke="#2a3547" stroke-width="1" stroke-dasharray="4" />
                    <line x1="500" y1="0" x2="500" y2="500" stroke="#2a3547" stroke-width="1" stroke-dasharray="4" />
                    <!-- Region Markers -->
""");
        sb.append(svgMarkers);
        sb.append("""
                  </svg>
                </div>

                <div class="card">
                  <table id="resultsTable">
                    <thead>
                      <tr>
                        <th>Status</th>
                        <th>City</th>
                        <th>Country</th>
                        <th>Continent</th>
                        <th>Median</th>
                        <th>p95</th>
                        <th>Loss</th>
                        <th>Efficiency</th>
                        <th>Host / Error</th>
                      </tr>
                    </thead>
                    <tbody>
""");
        sb.append(rows);
        sb.append("""
                    </tbody>
                  </table>
                </div>
              </div>

              <script type="application/json" id="nardol-raw-json">
""");
        sb.append(json);
        sb.append("""
              </script>
            </body>
            </html>
""");

        return sb.toString();
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
