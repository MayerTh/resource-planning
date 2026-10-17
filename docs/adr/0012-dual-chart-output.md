# ADR 0012: Reports render both inline-terminal and as an interactive HTML file

- Status: accepted
- Date: 2026-10-11

## Context

Reports/charts (spare capacity per member, staffing gaps per project — ADR 0004/0010) need an output medium. Options were terminal-only (ASCII/Unicode), file-only, or both. We also needed to decide the file format if "file" was in scope: static image (PNG/SVG) vs. HTML.

## Decision

**Both.** The CLI renders reports inline in the terminal (ASCII/Unicode tables/bars, for a quick look without leaving the shell) and can also export an **HTML file** for the same report, with light interactivity (e.g. sortable/filterable tables) — not a static image.

## Alternatives considered

- **Terminal only**: rejected — no good way to share a report or view something richer than monospace text (e.g. sorting a long member list).
- **Static image (PNG/SVG) export**: rejected in favor of HTML — HTML gets light interactivity for free (sorting, filtering) at similar implementation cost, and is easier to view/share (opens in any browser) than managing image file viewers.
- **File only (drop terminal)**: rejected — a quick terminal glance during day-to-day planning shouldn't require opening a file each time.

## Consequences

- Need two renderers over the same underlying report/statistics data: a terminal renderer and an HTML renderer. Keeping the report computation (ADR 0004/0010) decoupled from both is important so neither renderer duplicates logic.
- HTML interactivity implies a small amount of client-side JS/CSS in the generated file — kept light (sort/filter), not a full web app.
- Exact charting approach (e.g. plain tables vs. bar/heatmap visuals, which JS, if any, to embed) is an implementation detail for later, not decided here.
