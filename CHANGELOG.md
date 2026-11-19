# Changelog

All notable changes to **NARDOL** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.2.0] - 2026-11-19

### Added
- **Fresh Run Cache Control**:
  - `--clear-cache` flag for CLI invocations (`nardol` / `nardol run`) to bypass cached lists and history for clean runs.
  - Subcommand `nardol history --clear` to clear saved measurement history on demand.
  - Implemented `ListCache.clear()` and `HistoryStore.clear()`.
- **TUI Verbose Mode**:
  - Added `-v` / `--verbose` flag support to interactive TUI (`nardol tui -v`).
  - Integrated detailed round-trip telemetry (loss %, jitter stddev, p95 latency) in TUI display tables.

### Fixed
- **TUI Toggle Map (`M`) Rendering**:
  - Fixed map view not displaying in the terminal by overloading `WorldMap.render` with a `PrintWriter` directed into JLine's alternate screen buffer.
- **HTML Report Map Rendering**:
  - Added vector land geometry generator `WorldMap.generateSvgLandPath(1000.0, 500.0)` enabling HTML reports to render offline world maps without broken external assets.

### Changed
- **Target List Updates (`targets.tsv`)**:
  - Formatted, normalized, and validated endpoints across 64 global regions with 0 errors.
  - Updated mirrors and provider endpoints for Cairo, Bangkok, Dallas, Perth, and Auckland.
  - Re-signed bundled resource with Ed25519 signatures and SHA-256 checksums.

## [2.1.0] - 2026-11-15

### Added
- **Dynamic & Daily Remote Target Updates**:
  - Configurable primary and backup target URLs loaded dynamically from `version.properties`.
  - Configured primary source endpoint: `https://explodenot.com/nardol/target.tsv`.
  - Automatic fallback to backup mirror sources and bundled lists if the primary is unreachable.
  - Daily freshness validation supporting conditional `ETag` and `If-None-Match` checks.
  - On-demand updates via `nardol list update`.
  - Subcommand `nardol list sources` updated to display active primary and backup URLs.
- **Enhanced Verbose Mode (`-v` / `--verbose`)**:
  - Full propagation of `--verbose` to `run` subcommand (`nardol run -v`).
  - Detailed telemetry rows rendering p95 latency, jitter stddev, loss %, and TLS version & cipher suite (e.g. `[TLSv1.3/TLS_AES_128_GCM_SHA256]`).
  - Detailed fallback diagnostics in verbose mode displaying the primary failure reason when switched to fallback candidates.
- **Robust Multi-Provider Pre-Flight Control Checks**:
  - Pre-flight DNS checks resolve across multiple major public resolvers (Cloudflare, Google, Quad9, Cisco Umbrella).
  - Pre-flight raw IP reachability expanded across Cloudflare, Google, Quad9 (`9.9.9.9`), and Cisco Umbrella (`208.67.222.222`) over ports 53 and 443, with dual-stack IPv6 probes (`2606:4700:4700::1111`, `2001:4860:4860::8888`, `2620:fe::fe`, `2620:119:35::35`).
- **Security Key Management & Git History Hygiene**:
  - Completely purged private key artifacts from git commit history via `git filter-branch` and garbage collection.
  - Added repository-wide `.gitignore` protection for `tools/keys/` and `*.key`.
  - Upgraded target signing workflow to use externalized environment secrets (`NARDOL_PRIVATE_KEY_BASE64` or `tools/keys/ed25519-private.key`).
  - Generated fresh, cryptographically isolated Ed25519 keypair and re-signed default `targets.tsv.sig`.
  - Added GitHub Actions Secrets setup instructions to `TECHNICAL.md`.
- **Bogon & Private IP Protection**:
  - Added strict SSRF and LAN isolation preventing targets or resolved destinations from probing private, loopback, link-local (cloud metadata `169.254.169.254`), CGNAT, or reserved bogon IPs.
  - Rejection of bogon IP literals in `TargetValidator` and classification as `ProbeOutcome.Failure.Blocked`.
- **Automated Workflow Hardening**:
  - Converted target list health check (`list-health.yml`) to run daily at 04:00 UTC.
  - Pinned all GitHub Actions steps in CI, release, and health check workflows to immutable 40-character commit SHAs.
  - Configured `.semgrepignore` and source-level suppressions for static application security testing (SAST).

### Fixed
- **Java 22+ Restricted Method Warnings**:
  - Eliminated `WARNING: A restricted method in java.lang.System has been called` by prioritizing standard JLine JNI terminal providers over Jansi.
  - Added `"Enable-Native-Access" to "ALL-UNNAMED"` in jar manifest and launcher scripts.
- **JLine Deprecated Provider Warning**:
  - Suppressed deprecated provider log warnings via `System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")`.
- **Package Refactoring**:
  - Fully refactored project package namespaces from `com.explodenot.gli` to `com.explodenot.nardol`.

---

## [2.0.0] - 2026-11-15

### Added
- Initial release of **NARDOL** v2 ("Light the beacons, see who answers").
- Layer-4 / Layer-7 probing engine supporting TCP, TLS (SNI), and HTTP (HEAD/Range GET).
- Virtual-thread concurrency model gated by global and per-domain semaphores.
- Physics-aware efficiency calculations comparing measured RTT against fiber optic theoretical limits ($2 \times \text{distance} / 200\text{ km/ms}$).
- Sub-floor Anycast / CDN detection.
- Local history in JSON Lines format (`~/.cache/nardol/history.jsonl`) with trend sparklines.
- Heuristic continental correlation anomaly detector.
- Full-screen alternate-screen terminal dashboard (`nardol tui`).
- Terminal world map (`nardol map`) using compact rasterized land mask.
- Self-contained offline HTML reports (`nardol report <file.html>`).
- Cryptographic Ed25519 signature and SHA-256 validation for target lists.
- Anti-regression build guards and automated documentation consistency tests.
