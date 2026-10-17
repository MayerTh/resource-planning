# ADR 0003: Static calendar reference file for Bavarian working days

- Status: superseded by [ADR 0005](0005-working-days-counts-only.md)
- Date: 2026-09-23

## Context

Capacity calculations (how much a person can work in a given month) need to know the number of working days per month. "Working day" here means Mon–Fri excluding Bavarian public holidays — not derivable from weekday logic alone, so it needs to be stored as data rather than computed at runtime.

## Decision

A fourth file, `data/calendar.toml`, holds working-day data for **2026, 2027, and 2028**, scoped to **Bavaria, Germany**. Unlike the other three files, this one is treated as generated-once, read-only reference data rather than something regularly hand-edited.

Format: TOML (per [ADR 0001](0001-toml-ground-truth-files.md)), one `[[working_days]]` entry per year/month, storing the **full list of working-day dates** (not just a count) — a count can always be derived from the list, but the reverse isn't true, and the full list is needed if assignments later need partial-month precision.

```toml
[[working_days]]
year = 2026
month = 1
days = ["2026-01-02", "2026-01-05", ...]
```

Holiday data source: the 13 statewide Bavarian public holidays (Neujahr, Heilige Drei Könige, Karfreitag, Ostermontag, Tag der Arbeit, Christi Himmelfahrt, Pfingstmontag, Fronleichnam, Mariä Himmelfahrt, Tag der Deutschen Einheit, Allerheiligen, 1. and 2. Weihnachtsfeiertag), dates verified via web search against arbeitstage.org for 2026–2028.

## Alternatives considered

- **Compute working days at runtime** from weekday + a hardcoded holiday-calculation algorithm: rejected because Easter-based holidays require a computus algorithm and the result should be reviewable/correctable as plain data, not buried in code.
- **Store only a per-month count**: simpler, but loses information needed for exact-date partial-month allocation later.

## Consequences

- **Mariä Himmelfahrt (Aug 15)** is legally a public holiday only in Bavarian municipalities with a majority-Catholic population, not uniformly statewide. It is treated as non-working for the whole state here for simplicity. This only affects the computed data in **2028** (Aug 15 falls on a Tuesday that year); in 2026 and 2027 it falls on a weekend regardless. If the team isn't in a majority-Catholic municipality, this file needs a manual correction for 2028-08.
- Extending coverage to a new year (2029+) or a different German state requires regenerating/editing this file — there's no runtime calendar logic to fall back on.
- If a company-specific non-working day (e.g. an office closure) needs to be added, it goes here too, as a manual edit.
