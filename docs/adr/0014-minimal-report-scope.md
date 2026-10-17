# ADR 0014: Keep the set of reports small, with simple/flat structure

- Status: accepted
- Date: 2026-10-11

## Context

ADR 0004 and 0010 define *what* is computed (spare capacity per member, staffing gaps per project, over-/under-allocation flags) but not how many distinct report views the tool exposes, or how complex their internal structure is. Left unconstrained, a reporting module tends to accumulate overlapping report variants and deeply nested/configurable formats over time.

## Decision

Keep the reporting surface **small and simple**:

- A small, fixed number of report types — in line with ADR 0004/0010: a per-member capacity/spare-time view, a per-project gap/skills view, and a combined overview. No more unless a concrete need arises.
- **Flat, tabular structure** for each — rows of members or projects with a handful of columns — rather than deeply nested or user-configurable report formats.
- New report types or added structural complexity are justified by an actual need when it arises, not added speculatively ahead of time.

This applies to both output media from ADR 0012 (terminal and HTML) — the HTML version's "light interactivity" means sort/filter on these same flat tables, not a distinct, richer report structure.

## Alternatives considered

- **Configurable/flexible reporting framework** (user-defined templates, many report variants): rejected — disproportionate complexity for a single-user tool (ADR 0011) answering a small, known set of questions ("who's free," "what's understaffed").

## Consequences

- The report/visualization code stays easy to read and maintain.
- If more report variety turns out to be needed later, it's added incrementally against real demand, not designed in up front.
