# Architectural and Dependency Decisions

## 1. Allowed Runtime Dependencies & Size Justification
To keep the fat jar well under the 6 MB target (hard limit 10 MB) while providing rich terminal UI capabilities:
- **`info.picocli:picocli` (~415 KB)**: Complete CLI command parsing, subcommand routing, typed options, ANSI help formatting, and completion generation without external reflection overhead.
- **`org.jline:jline-terminal` (~265 KB) & `org.jline:jline-terminal-jansi` (~50 KB) with `org.fusesource.jansi:jansi` (~257 KB)**: Provides robust terminal detection, raw mode, alternate screen buffer, key handling, and Windows ANSI support across macOS, Linux, and Windows (x64 and arm64). Unneeded native libraries (FreeBSD, 32-bit Linux/Windows/macOS, MIPS, PPC, RISC-V, s390x) are excluded in `shadowJar`, shaving native bloat.
- **`com.google.code.gson:gson` (~286 KB)**: Chosen over Jackson (~2.5+ MB for core + databind). Gson provides fast, zero-dependency JSON serialization and deserialization for JSON Lines history, remote target lists, and configuration with tiny byte footprint.
- **Total runtime dependency footprint before packaging**: ~1.41 MB.
- **Resulting minimized fat jar size**: ~1.56 MB, comfortably under the 6.0 MB budget and well below the 10.0 MB hard limit.

## 2. JLine Terminal Fallback
If JLine or its native provider fails to initialize (e.g. unsupported platform or security manager restrictions), Nardol cleanly catches any exception, logs a one-line notice, and falls back to standard ANSI / plain stream output without crashing or blocking execution.

## 3. Land Mask Representation
Rather than bundling a large vector map or multi-megabyte GeoJSON, we rasterized Natural Earth's 110m low-resolution land boundaries into a 360×180 1-degree equirectangular boolean bitmap. Zlib-compressed, this binary artifact is only ~1.8 KB (`world-mask.bin`). When rendered to the terminal, it is upscaled/downscaled dynamically using Unicode Braille patterns (2×4 sub-cells) or standard ASCII characters when `--ascii` is passed.

## 4. History Storage & No Backward Compatibility Shims
In accordance with constraints prohibiting JDBC/SQLite/H2 native bloat, historical runs are appended as compact JSON Lines (`history.jsonl`) under the user's cache directory (`$XDG_CACHE_HOME` or `~/.cache/nardol`, `~/Library/Caches/...` on macOS, `%LOCALAPPDATA%` on Windows). History files automatically rotate to keep the last 100 runs.
As the project is unreleased prior to v2.0.0, no backward-compatibility shims or fallback migrations for legacy environment variables or previous cache paths are provided.

## 5. Security and List Verification
Target lists are parsed strictly as pure data with length, character-set, port, and latitude/longitude validation. Remote list downloads are signed via Ed25519 (`targets.tsv.sig`) against a bundled public key, with SHA-256 fallback and `--insecure-skip-verify` for local testing. All HTML output escapes user/remote strings to prevent injection.

## 6. Remote Target Update Strategy & Source Fallbacks (v2.1.0)
To ensure long-term availability and flexibility without requiring code re-releases:
- Target endpoint locations are decoupled into `version.properties` (`targets.url.primary`, `targets.url.backup1`, etc.).
- The client performs freshness checking against cached lists using a 24-hour interval threshold alongside conditional HTTP headers (`If-None-Match` / `If-Modified-Since`).
- Mirror failover is strict: if the primary source (`https://explodenot.com/nardol/target.tsv`) is unreachable or returns an HTTP error, secondary backup URLs are sequentially attempted before falling back to local cache or the offline bundled fallback list.

## 7. Java Native Access & Terminal Provider Deprecation Handling (v2.1.0)
Starting in modern OpenJDK releases (Java 22+), direct calls to native methods such as `System.loadLibrary` trigger restricted method warnings if native access is not granted.
- Nardol configures `Enable-Native-Access: ALL-UNNAMED` in `shadowJar` manifests and passes `--enable-native-access=ALL-UNNAMED` in launcher scripts (`nardol`, `nardol.cmd`, `nrd`).
- JLine terminal initialization prioritizes the modern JLine JNI terminal provider (`.jansi(false).jni(true)`), suppressing deprecated Jansi provider warnings with `org.jline.terminal.disableDeprecatedProviderWarning=true`.
