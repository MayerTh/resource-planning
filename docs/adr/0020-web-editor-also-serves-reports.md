# ADR 0020: Web editor also serves the HTML report

- Status: accepted
- Date: 2026-10-17
- Supersedes the "website also takes over reporting: rejected" alternative in [ADR 0016](0016-web-editor-second-interface.md), and the "stays CLI-only" framing of the non-goal in [PROJECT.md](../../PROJECT.md)

## Context

ADR 0016 deliberately kept the web editor to editing only, rejecting a reports view to avoid building/maintaining two HTML-rendering paths for the same data. In practice, once the web editor is already open in a browser on the local network, it's natural to want to see the report there too, instead of separately running the CLI to get `report.html`.

## Decision

The web editor gains a **read-only `/reports` page** that renders the same report the CLI exports, by reusing the exact same renderer — no second rendering implementation:

- `HtmlReportRenderer` (previously in `cli`) moves to `core` (`org.resourceplanning.report`), alongside the computation layer it renders (`CapacityCalculator`, ADR 0015). It has no framework dependency, so this is a pure relocation, not a rewrite.
- `cli`'s `Main` keeps using it unchanged (via the `core` dependency it already has) to write `report.html`.
- `web-editor`'s new `ReportsResource` calls `CapacityCalculator` + `HtmlReportRenderer` directly and returns the resulting HTML as the response body — the same self-contained document (own `<html>`/`<style>`/sort script) a CLI export produces, just served live instead of written to a file.
- `TerminalReportRenderer` stays in `cli` — the terminal view remains CLI-only; only the HTML report gets a second consumer.

## Alternatives considered

- **Keep reporting CLI-only** (status quo per ADR 0016): rejected on reconsideration — the inconvenience of switching to a terminal just to regenerate `report.html` outweighs the module-boundary purity, now that there's an actual user wanting this.
- **Separate Qute-templated reports page in `web-editor`**: rejected — would mean two independent HTML-rendering implementations for the same report, the exact duplication ADR 0016 was trying to avoid. Reusing `HtmlReportRenderer` directly avoids that at negligible cost (it was already framework-free).
- **Move the renderer into a new shared module**: rejected as unnecessary — `core` already has no framework dependencies and is already a shared dependency of both `cli` and `web-editor`, so it's the natural home.

## Consequences

- `core` now also owns the HTML rendering of reports, not just domain/computation/IO (slight widening of ADR 0015's scope for `core`, but not a new module).
- PROJECT.md's non-goal "no GUI for reporting" is removed — both interfaces can now show reports; only the *editing* scope split from ADR 0016 (web-editor edits `team_members.toml`/`planning.toml`, not `working_days.toml`) still stands.
- ADR 0012/0014 (dual terminal+HTML output, minimal flat report scope) are otherwise unaffected — the web editor's `/reports` page is a second consumer of the same HTML output, not a new report type or structure.
- No new tests duplicate rendering logic: `HtmlReportRendererTest` moved to `core` unchanged; `web-editor`'s test suite only asserts the `/reports` route serves that same output.
