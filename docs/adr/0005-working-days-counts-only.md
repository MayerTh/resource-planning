# ADR 0005: Working-days file stores counts only, renamed to `working_days.toml`

- Status: accepted
- Date: 2026-09-23
- Supersedes: [ADR 0003](0003-calendar-reference-file.md)

## Context

ADR 0003 introduced `data/calendar.toml`, storing the full list of working-day dates per month for Bavaria, 2026–2028. With [ADR 0004](0004-manual-planning-with-gap-visualization.md) and [ADR 0006](0006-single-planning-file.md) settling that planning happens at **monthly granularity only** (no day-level precision anywhere in the tool), the full date lists are more detail than anything actually needs — only the count of working days per month is used in any calculation.

## Decision

- Rename `data/calendar.toml` → `data/working_days.toml`.
- Change the schema from a `days` list to a single `count` integer per year/month:

```toml
[[working_days]]
year = 2026
month = 1
count = 20
```

The underlying source data (13 Bavarian statewide public holidays, verified for 2026–2028) and the Mariä Himmelfahrt caveat from ADR 0003 still apply unchanged — only the stored granularity changes.

## Alternatives considered

- **Keep the full date list "just in case"**: rejected (YAGNI) — nothing in the current design uses day-level precision, and the generation logic is documented well enough to regenerate a date-level version later if a future feature needs it.

## Consequences

- `data/calendar.toml` is deleted; `data/working_days.toml` replaces it.
- Any future feature needing day-level precision (e.g. exact-date partial-month assignments) would need to either regenerate the date-level data or recompute it from the holiday list.
