# globalping-lite (gpl) v2

A feature-rich, cross-platform Java 21 CLI and TUI reachability probe that measures real network connectivity and latency from the user's machine to physically distributed servers across the globe.

---

## Features
- **Zero Config, Zero API Keys, Runs Out of the Box**: Single executable fat jar (`java -jar gpl.jar`).
- **Modern Java 21**: Virtual threads for high probe concurrency, records, sealed interfaces, and pattern matching.
- **Physical Unicast Region Probes**: Probes region-pinned endpoints (AWS, Oracle Cloud, Linode, Vultr, Hetzner, universities) with zero anycast CDN cheating.
- **Multi-Mode Probes**:
  - `tcp` (default): Separate DNS resolution timing + timed TCP connection.
  - `tls`: TLS handshake with SNI using `SSLSocket`, measuring handshake time and negotiated protocol/cipher.
  - `http`: HTTP `HEAD` (fallback `GET` with Range: 0-0) measuring time to first byte and HTTP status codes.
- **Physics-Aware Efficiency**: Haversine distance from user location against fiber-optic physical limit ($c_{fiber} \approx 200\text{ km/ms}$). Flags suspicious anycast or detour routes.
- **History & Correlation Alerts**: Tracks runs in JSON Lines (`history.jsonl`) with trend sparklines and continent-wide correlation degradation heuristic alerts.
- **Rich Visuals**:
  - Grouped terminal reports with truecolor gradients, latency bars, and Unicode/ASCII fallback glyphs.
  - Interactive full-screen terminal TUI dashboard (`gpl tui`).
  - Terminal world map (`gpl map`) rendered from a compact bundled land mask (<2 KB).
  - Standalone self-contained HTML report (`gpl report <file.html>`) with SVG global maps, zero external network requests.
- **Ultra-Lean Footprint**: Fat jar size is ~1.56 MB (well below the 6.0 MB target and 10.0 MB hard limit).

---

## Installation & Running

### Requirements
- Java 21+ LTS runtime (`java -version`).

### Build from source
```bash
./gradlew build
```
The executable jar is produced at `build/libs/gpl-2.0.0-all.jar`.

### Run
```bash
# Default run: probes worldwide regions and outputs terminal report
java -jar build/libs/gpl-2.0.0-all.jar

# Interactive full-screen TUI
java -jar build/libs/gpl-2.0.0-all.jar tui

# Render world map
java -jar build/libs/gpl-2.0.0-all.jar map

# Generate offline self-contained HTML report
java -jar build/libs/gpl-2.0.0-all.jar report reachability.html
```

---

## Command Reference

### Root Options
```
Usage: gpl [-qvV] [--all-candidates] [--allow-insecure-list] [--ascii]
           [--compact] [--csv] [--debug] [--dual-stack] [--force]
           [--insecure-skip-verify] [--ipv4] [--ipv6] [--json] [--ndjson]
           [--no-color] [--no-history] [--no-update] [--offline]
           [--prefer-ipv6] [--color=<colorMode>]
           [--color-depth=<explicitColorDepth>] [--concurrency=<concurrency>]
           [--count=<watchCount>] [--lat=<userLat>] [--list-file=<listFile>]
           [--list-url=<listUrl>] [--lon=<userLon>] [--mode=<mode>]
           [--port=<portOverride>] [--provider=<provider>]
           [--samples=<samples>] [--sort=<sortOrder>] [--timeout=<timeoutMs>]
           [--watch=<watchIntervalSeconds>] [--exclude=<excludes>]...
           [--list-mirror=<listMirrors>]... [--region=<regions>]... [COMMAND]
```

### Subcommands
- `gpl run`: Probe and render terminal report (default command).
- `gpl tui`: Interactive full-screen terminal dashboard.
- `gpl map`: Terminal world map of reachability.
- `gpl report <file.html>`: Generate self-contained offline HTML report with embedded JSON.
- `gpl history`: Summaries, trends, and `--export json|csv` of past runs.
- `gpl list`: Manage target list (`show`, `update`, `validate <file>`, `sources`).
- `gpl doctor`: Diagnose local network, DNS, proxies, permissions, and terminal capabilities.
- `gpl completion bash|zsh|fish`: Generate shell autocompletion.
- `gpl config show|set|reset`: Manage per-user default preferences.

---

## Target List Schema & Contribution Rules

The canonical target list is TSV format located at `src/main/resources/targets.tsv`:
```tsv
# Columns: region_id<TAB>continent<TAB>country<TAB>city<TAB>provider<TAB>host<TAB>port<TAB>lat<TAB>lon<TAB>tier
na-us-east-va	North America	US	Ashburn	aws	dynamodb.us-east-1.amazonaws.com	443	39.04	-77.49	primary
na-us-east-va	North America	US	Ashburn	hetzner	ash-speed.hetzner.com	443	39.04	-77.49	fallback
```

### Contribution Rules:
1. **Unicast Only**: No Anycast IPs (no 1.1.1.1, 8.8.8.8, Cloudflare CDN frontends). Targets must be geographically pinned to the stated city.
2. **Ordered Fallback Groups**: Entries sharing `region_id` form a group; exactly one `primary` per group (first line).
3. **Validation**: Run `./gradlew test` and `gpl list validate <file>` before opening PRs.
4. **Signing**: Target lists are signed using Ed25519 (`tools/sign-list <file>`).

---

## Exit Codes
- `0`: All regions reachable.
- `1`: Some regions degraded or down.
- `2`: All regions down or initial control checks failed.
- `64`: Usage or validation error.
- `70`: Internal execution error.

---

## Limitations & Privacy

### Limitations
- **No ICMP**: We do not use ICMP ping or raw sockets, avoiding root/setuid privileges.
- **L4/L7 vs App State**: A successful TCP connect or TLS handshake proves network reachability to the regional endpoint, not necessarily backend service health.
- **Environmental Distortion**: VPNs, corporate proxies, and WSL2 NAT can distort latency measurements. `gpl doctor` and pre-run control checks flag these conditions.
- **Censorship / Geoblocks**: Regional firewalls (e.g. Great Firewall) may block certain foreign cloud endpoints.

### Privacy
- **Zero Telemetry**: `globalping-lite` collects and transmits zero analytics or telemetry.
- **Network Boundaries**: The only outbound network requests made are:
  1. The target endpoints specified in the target list.
  2. The target list update URL (can be disabled via `--no-update` or `--offline`).
