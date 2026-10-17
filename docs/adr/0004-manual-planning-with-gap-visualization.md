# ADR 0004: Manual assignment planning, with computed gap/spare-capacity visualization; optimization deferred

- Status: accepted
- Date: 2026-09-23

## Context

The tool could either do the assignment planning itself (an optimizer that assigns people to projects) or leave that decision to the head of department, with the tool only supporting the decision. We discussed the tradeoff directly.

## Decision

**Planning stays manual for now.** The user enters assignments themselves (in `data/planning.toml`, see [ADR 0006](0006-single-planning-file.md)). The tool's job is to **compute and visualize**, not decide:

- **Spare/unplanned capacity per team member per month**: originally hours-based here; superseded by the percentage-based formula in [ADR 0008](0008-percentage-based-assignments.md) (`100 − sum(assigned percentage that month)`).
- **Staffing gap per project**: `project.hours_total` spread evenly across the project's working days in its `[start_month, end_month]` range (using `data/working_days.toml`) to get an implied monthly need, compared against assigned effort for that project that month. **Resolved by [ADR 0010](0010-percentage-based-gap-reporting.md)**: this hours-based comparison was dropped in favor of a purely percentage/skill-based gap — `hours_total` ended up reference-only, not read by the gap computation.

The computation/visualization logic is kept as a separate layer from the data files, reading `data/team_members.toml`, `data/working_days.toml`, and `data/planning.toml`. This is deliberate: a **future optimization mode** (auto-suggesting or auto-filling assignments) can be added later as an alternative writer of the `[[assignment]]` entries in `data/planning.toml`, using the same three files as input, without changing the file formats or the visualization logic.

### Over-allocation and under-allocation reporting

Both are handled **in the output only** — the tool never blocks or rejects an assignment in the data file, it only flags:

- **Over-allocation**: a team member whose assigned percentage across all projects exceeds 100% in a month is flagged in the output/report (see [ADR 0008](0008-percentage-based-assignments.md)).
- **Under-allocation** is reported symmetrically, from both directions:
  - **Per project**: the staffing gap is shown together with the project's `skills_needed` — "this project needs more capacity for skill X" (exact metric still open, see above).
  - **Per team member**: spare percentage/hours are shown together with that member's `skills` — "this person has N% spare capacity, with skills Y."

### Personal absence (vacation/PTO) is out of scope

Capacity is computed purely from `data/working_days.toml` (Bavarian public holidays) and `hours_daily` — individual vacation/PTO is not tracked or subtracted from capacity anywhere in the tool.

## Alternatives considered

- **Full optimizer from the start**: rejected — more complex than needed for an MVP, and the user explicitly wants manual control first.
- **Manual planning with no computed visualization**: rejected — contradicts the explicit requirement to see unplanned hours and staffing gaps.

## Consequences

- Need a capacity/gap calculation module that reads all three data files; this module is the natural seam where a future optimizer would plug in.
- "Rough estimate" is the intended precision level (monthly granularity, evenly-spread project demand) — not day-level precision.
