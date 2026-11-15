# Changelog

All notable changes to **NARDOL** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

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

### Fixed
- **Java 22+ Restricted Method Warnings**:
  - Eliminated `WARNING: A restricted method in java.lang.System has been called` by prioritizing standard JLine JNI terminal providers over Jansi.
  - Added `"Enable-Native-Access" to "ALL-UNNAMED"` in jar manifest and launcher scripts.
- **JLine Deprecated Provider Warning**:
  - Suppressed deprecated provider log warnings via `System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")`.

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
