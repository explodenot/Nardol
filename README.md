# NARDOL

> **Light the beacons, see who answers.**

Cross-platform Java 21 CLI and TUI reachability probe measuring real connectivity and latency from your machine to physically distributed, region-pinned unicast servers worldwide.

[![CI Build](https://img.shields.io/github/actions/workflow/status/<OWNER>/<REPO>/ci.yml?branch=main&label=CI)](https://github.com/<OWNER>/<REPO>/actions)
[![Release](https://img.shields.io/github/v/release/<OWNER>/<REPO>?label=release)](https://github.com/<OWNER>/<REPO>/releases)
[![Java 21+](https://img.shields.io/badge/Java-21%2B%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Platforms](https://img.shields.io/badge/platforms-Linux%20%7C%20macOS%20%7C%20Windows-blue.svg)](#quick-start)
[![Jar Size](https://img.shields.io/badge/jar%20size-%3C%201.6%20MB-success.svg)](#quick-start)
[![No Telemetry](https://img.shields.io/badge/telemetry-none-brightgreen.svg)](#security-and-privacy)

---

<!-- Demo placeholder: captured terminal recording (run, tui, map) -->
<!-- Owner instructions: Add terminal recording or asciinema cast to docs/img/demo.svg or link here -->

```text
 _  _   _   ___ ___   ___  _     
| \| | /_\ | _ \   \ / _ \| |    
| .` |/ _ \|   / |) | (_) | |__  
|_|\_/_/ \_\_|_\___/ \___/|____| 
Light the beacons, see who answers.

─── North America (1) ───
▲  Ashburn, US            509.4 ms ████████████            ↑+214.1ms █ ▂   ▆ aws (dynamodb.us-east-1)

╭──────────────────────────────── Summary ────────────────────────────────╮
│ Reachability: 1/1 reachable · 1 degraded · 0 down
│ Fastest: na-us-east-va (509.4 ms)   Slowest: na-us-east-va (509.4 ms)   Median of Medians: 509.4 ms
│ List: bundled fallback list (v2.1.0) · Total probe time: 536 ms
╰─────────────────────────────────────────────────────────────────────────╯
```

---

## Table of Contents
- [Why This Exists](#why-this-exists)
- [Quick Start](#quick-start)
- [What It Measures (and What It Doesn't)](#what-it-measures-and-what-it-doesnt)
- [Reading the Output](#reading-the-output)
- [Features](#features)
- [Command Reference](#command-reference)
- [Cookbook](#cookbook)
- [Configuration](#configuration)
- [The Target List](#the-target-list)
- [Security and Privacy](#security-and-privacy)
- [How It Compares](#how-it-compares)
- [Limitations and FAQ](#limitations-and-faq)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)
- [Name Note](#name-note)

---

## Why This Exists

When an overseas application or API is sluggish, answering whether the issue stems from your local Wi-Fi, your ISP transit, an ocean cable anomaly, or a whole geographic cloud region normally requires juggling multiple dashboards. Nardol measures connectivity *from your own network*, comparing actual performance against *your own historical baseline* and the theoretical limits imposed by the speed of light in fiber optics. It requires zero API keys, no account, zero configuration, and operates out of the box with zero external telemetry.

---

## Quick Start

### Requirements
- Any **Java 21+ LTS** runtime (`java -version`). No native package managers or dependencies needed.

### 1. Download & Run
Download the latest fat jar from GitHub Releases:
```bash
java -jar nardol-2.1.0-all.jar
```

### 2. Build from Source
```bash
git clone https://github.com/<OWNER>/<REPO>.git
cd gli
./gradlew shadowJar
java -jar build/libs/nardol-2.1.0-all.jar
```

### 3. Launcher Scripts & PATH
Executable wrapper scripts `nardol` and `nrd` (and `nardol.cmd` for Windows) are provided in the repo. To install system-wide:
```bash
# Linux / macOS
cp nardol ~/.local/bin/nardol
cp nrd ~/.local/bin/nrd

# Shell completion (bash, zsh, fish)
./nardol completion bash > ~/.bash_completion.d/nardol
```

---

## What It Measures (and What It Doesn't)

- **What it measures**: Layer-4/Layer-7 round-trip reachability (timed DNS resolution, TCP handshake, TLS handshake time, and HTTP time-to-first-byte) from your machine to physically located, region-pinned unicast servers.
- **What it does NOT measure**:
  - It is **not** an ICMP ping tool (no raw sockets or root privileges required).
  - It is **not** an application performance or bandwidth speed test.
  - It is **not** a diagnostic replacement for hop-by-hop traceroute (`mtr`) or distributed vantage probes (`RIPE Atlas`, `globalping.io`).
  - Correlation alerts are **heuristics**, not definitive proof of physical cable cuts.

---

## Reading the Output

```text
▲  Ashburn, US            509.4 ms ████████████            ↑+214.1ms █ ▂   ▆ aws (dynamodb.us-east-1)
```

1. **Status Icon**:
   - `✔` / `[OK]`: Reachable, operating normally within baseline thresholds.
   - `▲` / `[SLOW]`: Degraded (measured latency >50% slower than your median baseline or efficiency <25%).
   - `✖` / `[FAIL]`: Down or unreachable.
   - `⚠` / `[WARN]`: Heuristic warning (e.g. proxy detected or group correlation).
2. **City & Country**: Target geographic location.
3. **Median Latency & Bar**: Measured median round-trip time across samples with a color-coded bar.
4. **Efficiency %**: Ratio of theoretical fiber floor ($2 \times \text{distance} / 200\text{ km/ms}$) to measured median.
   - *Suspicious Detour*: Efficiency <10%, suggesting traffic is routing through distant peering points.
   - *Likely Anycast/CDN*: Measured latency is faster than the physics floor.
5. **Delta & Sparkline**: Comparison against your own historical runs with directional arrows and recent trend sparklines.
6. **Winning Host**: The primary or fallback endpoint that responded.

### Exit Codes
- `0`: All probed regions reachable.
- `1`: Some regions degraded or down.
- `2`: All regions down or initial control checks failed.
- `64`: Command-line usage or list validation error.
- `70`: Internal error.

---

## Features

- **Ordered Fallback Groups**: Multi-provider resilience (AWS, Oracle Cloud, Hetzner, Linode, Vultr, universities). Primary is probed first; fallbacks are tried if primary fails.
- **Three Probe Modes**: `tcp` (default connect), `tls` (handshake with SNI timing), `http` (HEAD/GET Range).
- **Dual-Stack Support**: `--ipv4`, `--ipv6`, `--prefer-ipv6`, and `--dual-stack` head-to-head comparison.
- **Local History & Baselines**: Compact JSON Lines store (`history.jsonl`) with automatic rotation.
- **Terminal UI (`nardol tui`)**: Alternate-screen live dashboard with keyboard navigation.
- **World Map (`nardol map`)**: Terminal world outline rendered from a compact embedded land mask.
- **Self-Contained HTML Report (`nardol report <file.html>`)**: Offline single-file report with SVG maps.
- **Machine-Readable Outputs**: Pipe-clean `--json`, `--ndjson`, and `--csv`.
- **System Doctor (`nardol doctor`)**: Diagnoses local DNS, raw IPs, proxies, and terminal capabilities.
- **Signed Remote Updates**: Ed25519-signed target list updates checking primary (`https://explodenot.com/nardol/target.tsv`) and backup sources daily or on-demand, with offline bundled fallbacks.

---

## Command Reference

### Subcommands

| Subcommand | Description |
|---|---|
| `run` | Probe and render report (default command) |
| `tui` | Live full-screen dashboard |
| `map` | Terminal world map of reachability and latencies |
| `report` | Generate a self-contained offline HTML report |
| `history` | Summaries, trends, and export of past probe runs |
| `list` | Manage, inspect, and validate the target regions list |
| `doctor` | Diagnose the local environment, network, and terminal |
| `completion` | Generate shell completion script (bash, zsh, fish) |
| `config` | View and modify user configuration defaults |

### Key Options

| Option | Description |
|---|---|
| `--region <id\|continent>` | Filter by region ID, continent, country, or city (repeatable) |
| `--exclude <id>` | Exclude region ID or continent (repeatable) |
| `--mode tcp\|tls\|http` | Probe mode: tcp (default), tls, or http |
| `--samples <int>` | Number of probe samples per target (default: 3) |
| `--timeout <ms>` | Per-probe timeout in milliseconds (default: 3000) |
| `--concurrency <int>` | Max concurrent probes (default: 64) |
| `--dual-stack` | Probe both IPv4 and IPv6 and show delta/winner |
| `--lat <val> / --lon <val>`| User coordinates for physics floor efficiency scoring |
| `--json / --ndjson / --csv`| Output machine-readable formats to stdout |
| `--theme <name>` | Theme: `default`, `beacon`, `mono`, `high-contrast`, `solarized`, `nord` |
| `--ascii` | Force ASCII glyphs instead of Unicode |
| `--no-color` | Disable ANSI color codes (also honors `NO_COLOR`) |
| `--offline` | Operate offline using cached/bundled target lists |

---

## Cookbook

```bash
# 1. Probe Europe and Asia regions only
./nardol run --region Europe --region Asia

# 2. Continuous watch mode updating every 5 seconds
./nardol run --watch 5 --count 10

# 3. Export JSON piped into jq for automated scripting
./nardol run --json --quiet | jq '.results[] | select(.lossPercent > 0)'

# 4. Purely offline execution using bundled list
./nardol run --offline

# 5. Measure TLS handshake reachability to AWS endpoints
./nardol run --mode tls --provider aws

# 6. Generate an offline HTML reachability report
./nardol report reachability.html

# 7. Diagnose local network and environment settings
./nardol doctor

# 8. Compare IPv4 vs IPv6 performance
./nardol run --dual-stack --region na-us-east-va

# 9. Validate a custom target list file
./nardol list validate custom-targets.tsv

# 10. Inspect historical run trends
./nardol history --export csv
```

---

## Configuration

Nardol stores configuration and cached data according to operating system standards:

| Operating System | Config Directory | Cache Directory |
|---|---|---|
| **Linux** | `~/.config/nardol` | `~/.cache/nardol` |
| **macOS** | `~/Library/Application Support/nardol` | `~/Library/Caches/nardol` |
| **Windows** | `%APPDATA%\nardol` | `%LOCALAPPDATA%\nardol` |

### Environment Variables
- `NARDOL_LIST_URL`: Override the remote target list URL (defaults to primary `https://explodenot.com/nardol/target.tsv` with configurable backup mirrors in `version.properties`).
- `NARDOL_LAT` / `NARDOL_LON`: Set your geographic coordinates for physics floor calculations.

---

## The Target List

The canonical list resides at `src/main/resources/targets.tsv`.

| Column | Type | Description |
|---|---|---|
| `region_id` | String | Unique slug (e.g. `na-us-east-va`) |
| `continent` | String | Continent name |
| `country` | String | ISO country code |
| `city` | String | Region city name |
| `provider` | String | Provider identifier (`aws`, `oci`, `vultr`, etc.) |
| `host` | String | Unicast hostname |
| `port` | Integer | Destination port (typically 443) |
| `lat` / `lon` | Double | Latitude and longitude of region |
| `tier` | Enum | `primary` or `fallback` |

### Contribution Rules
1. **Unicast Only**: No Anycast IPs (no 1.1.1.1, 8.8.8.8, Cloudflare CDN frontends).
2. **Ordered Fallback Groups**: Exactly one `primary` per group (first line).
3. **Lint Before PR**: Run `./nardol list validate <file>`.

---

## Security and Privacy

- **The List is Data, Never Code**: All fields are strictly validated against length, numeric, and character set bounds. Content is never passed to shell interpreters or reflection.
- **Ed25519 Signatures**: Remote list updates must verify against a bundled Ed25519 public key or matching SHA-256 checksum.
- **XSS-Safe HTML**: All dynamic strings in HTML reports are thoroughly escaped.
- **Zero Telemetry**: Nardol transmits zero analytics, tracking, or crash reports. Network calls are strictly limited to target endpoints and the specified list URL.

---

## How It Compares

| Feature | NARDOL | mtr / ping | gcping / cloudping | RIPE Atlas | globalping.io |
|---|---|---|---|---|---|
| **Vantage Point** | Your own network | Your own network | Your own network | Third-party probes | Third-party probes |
| **Hop-by-hop Analysis** | No | **Yes** (Best) | No | Optional | Optional |
| **Multi-Cloud Coverage** | **Yes** (AWS, OCI, etc.) | N/A | Single vendor | N/A | N/A |
| **Physics Efficiency Floor** | **Yes** | No | No | No | No |
| **Offline Operation** | **Yes** | **Yes** | No | No | No |
| **No Account / No Keys** | **Yes** | **Yes** | **Yes** | No | Optional |

*Note: Verify against each tool's current features.*

---

## Limitations and FAQ

- **Why not ICMP?** ICMP sockets on modern operating systems require root privileges or setuid binaries. TCP and TLS operate cleanly in user space.
- **Does TCP connect prove application health?** No. It proves network routing, reachability, and OS stack health.
- **Do VPNs or proxies affect results?** Yes. Traffic is routed through the VPN/proxy tunnel. Run `nardol doctor` to check for active proxies.
- **Why Java 21?** Java 21 provides Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`), enabling lightweight concurrency without thread-pool exhaustion.

---

## Roadmap

- Additional unicast cloud regions across emerging markets.
- Server-side collector mode reusing the JSON schema for fleet reachability.
- Richer terminal graph widgets for long-running watch sessions.

---

## Contributing

Contributions are welcome! Please run:
```bash
./gradlew check
```
Ensure all tests pass and `verifyNoLegacyNames` succeeds before opening a pull request.

---

## License

This project is licensed under the Apache License 2.0. See [THIRD-PARTY-NOTICES](src/main/resources/THIRD-PARTY-NOTICES) for third-party component licenses.

---

## Acknowledgements

- **Picocli** by Remko Popma for CLI parsing.
- **JLine 3** for cross-platform terminal control and raw mode.
- **Google Gson** for lightweight JSON serialization.
- **Natural Earth** for public domain world boundary data.

---

## Name Note

Nardol is one of the beacon-hills of Gondor, lit to pass the signal along the chain. This project is an independent hobby tool and is not affiliated with or endorsed by the Tolkien Estate.
