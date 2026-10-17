# ADR 0008: Assignment magnitude is a percentage of the member's own capacity, not hours

- Status: accepted
- Date: 2026-09-23
- Supersedes: the assignment-magnitude part of [ADR 0006](0006-single-planning-file.md) and the `hours` field in [ADR 0007](0007-nested-assignments-per-project.md)'s nested structure

## Context

ADR 0006 chose hours as the assignment unit for consistency with `hours_total` and `hours_daily`. In practice, planning is done in percentage-of-time terms ("Anna is 50% on this project this month"), which is also how the head of department already thinks about `pay_schema`/capacity. Raw hours can always be derived when needed for reporting; percentage is the more natural thing to hand-edit.

## Decision

Each entry in a project's `assignments` sub-table (ADR 0007) is `{ member, percentage }` instead of `{ member, hours }`:

```toml
  [project.assignments]
  "2026-01" = [ { member = "anna.mayer", percentage = 25 } ]
```

`percentage` is relative to **that member's own capacity for that month** — `percentage = 50` for `anna.mayer` means 50% of Anna's personal working hours that month (`working_days[year,month].count × anna.mayer.hours_daily`), *not* a flat/standardized FTE reference. This matters because `hours_daily` varies per person (e.g. Lea Fischer is part-time at 6h/day) — the same percentage means different absolute hours for different people.

Consequences for the capacity formulas from [ADR 0004](0004-manual-planning-with-gap-visualization.md):
- **Over-allocation**: `sum(percentage across all of a member's assignments in a month) > 100`. This needs no hours/working-days lookup at all — a pure percentage sum.
- **Spare capacity per member per month**: `100 − sum(assigned percentage that month)`, expressed as a percentage; convertible to hours for display via `spare% × hours_daily × working_days.count` if needed.
- Actual hours for any assignment, if needed for a report, are **derived, not stored**: `percentage / 100 × member.hours_daily × working_days[year,month].count`.

**Resolved by [ADR 0010](0010-percentage-based-gap-reporting.md)**: whether/how `hours_total` still feeds an automatic per-project staffing-gap estimate was left open here. ADR 0010 settled it — staffing-gap reporting stays purely percentage/skill-based, and `hours_total` remains in the project schema as a reference value only, never read by the gap computation.

## Alternatives considered

- **Flat/standardized FTE percentage** (e.g. `50` always means 4h regardless of the person): rejected — doesn't match how `hours_daily` (including part-time members) is meant to be used.
- **Keep hours** (status quo per ADR 0006/0007): rejected per explicit preference — percentage is the natural unit for how planning is actually done, and simplifies the over-allocation check to a pure percentage sum.

## Consequences

- `data/planning.toml` assignment entries use `percentage`, not `hours`.
- Any report/CLI output that wants to show hours must compute them from percentage using `data/working_days.toml` and the member's `hours_daily` — this computation happens at report time, not stored anywhere.
- The project-level staffing-gap computation (comparing `hours_total` against assigned effort) needed to be revisited now that assigned effort is percentage-based — resolved by [ADR 0010](0010-percentage-based-gap-reporting.md).
