# Changelog

All notable changes to AESTHENIXAI are documented here.

---

## [0.3.0] — 2026-05-02

### Added

- **Multi-language support** — Java, JavaScript, TypeScript, Python
- **Language-aware AI prompts** — backend uses language-specific prompt per file
- **Offline mode** — rule-only analysis, no AI call, instant results
- **Hybrid mode** — instant rule results shown while AI runs in background
- **Confidence scores** — each rule issue shows confidence % in hover tooltip
- **All-occurrences detection** — rule engine finds every matching line, not just the first
- **Flagged-snippet extraction** — backend sends only suspicious lines to AI, cutting latency 60–80%
- **Language normalization** — TypeScript normalized to JavaScript for backend
- **`mode` setting** — `hybrid` / `ai` / `offline` with descriptions
- **First-run onboarding** — welcome message on first activation
- **Proper `activationEvents`** — extension activates only for supported languages

### Changed

- Removed `System.out.println` debug logging from backend
- Rule patterns improved with tighter regex (fewer false positives)
- Python rules expanded: added `exec()` and file-without-context-manager
- JS/TS rules expanded: added null loose-check rule
- Java rules expanded: added large array allocation rule
- Context menus and keybindings now work on all 4 supported languages

---

## [0.2.0] — 2026-04-30

### Added

- **Inline completion** — Copilot-style ghost text on flagged lines (Tab to accept)
- **Accept/Reject fix commands** — `Ctrl+Shift+Enter` / `Ctrl+Shift+Backspace`
- **Metrics module** — tracks runs, latency, cache hits, errors in output channel
- **Partial re-analysis** — skips AI for minor edits (≤5 changed lines)
- **TTL cache** — results expire after 5 minutes, keyed by content hash + language
- **Cancellation** — user can press ✕ to cancel long-running AI analysis
- **Graceful fallback** — if AI fails, rule-based results stay visible
- **Output channel** — structured logging to "AESTHENIXAI" channel
- **Per-issue fix labels** — lightbulb shows specific fix text per issue type
- **`aesthenix.showMetrics`** command

### Changed

- Cache now uses `CachedEntry { result, timestamp }` for TTL support
- `reviewCode` accepts `CancellationToken` and `language` parameters
- `runLocalRules` accepts `language` parameter

---

## [0.1.0] — 2026-04-22

### Added

- Initial release
- Java code analysis via backend API
- Inline diagnostics with colored decorations
- Results panel (score, issues, suggestions)
- Diff preview before applying refactor
- Code action provider (lightbulb quick-fixes)
- Auto-analyze on save (opt-in)
- `aesthenix.analyze`, `aesthenix.analyzeSelection`, `aesthenix.applyRefactor`, `aesthenix.clearDiagnostics`
