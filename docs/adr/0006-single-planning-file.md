# ADR 0006: Single `planning.toml` file for projects and assignments, monthly granularity

- Status: partially superseded by [ADR 0007](0007-nested-assignments-per-project.md) (assignment storage structure — everything else here still applies)
- Date: 2026-09-23

## Context

Projects and assignments were originally planned as two separate ground-truth files. The user decided they want a single file for both, and that assignments should only ever be tracked at whole-month granularity (no day-level or percentage-of-day precision) — consistent with the "rough estimate" planning goal from [ADR 0004](0004-manual-planning-with-gap-visualization.md).

## Decision

One file, `data/planning.toml`, with two array-of-tables:

```toml
[[project]]
id = "internal-tools-revamp"       # slug derived from name, kebab-case
name = "Internal Tools Revamp"
start_month = "2026-01"             # YYYY-MM, month granularity only
end_month = "2026-06"
hours_total = 800                   # total hour budget for the whole project
skills_needed = ["java", "typescript"]
responsible = "Anna Mayer"          # free text, not required to be a team member

[[assignment]]
project_id = "internal-tools-revamp"   # references project.id
member_id = "anna.mayer"               # references team member id (ADR 0002)
year = 2026
month = 1
hours = 80                              # hours assigned to this member, this project, this month
```

Field notes:
- `id`: kebab-case slug of the project name, same rationale as [ADR 0002](0002-slug-id-for-team-members.md) — stable and human-readable, used as the foreign key from `[[assignment]]`.
- `responsible`: plain string (name), not a reference to a team member — the responsible person may be outside the roster (e.g. a stakeholder).
- `hours_total`: a single total budget for the whole project; the gap-visualization logic ([ADR 0004](0004-manual-planning-with-gap-visualization.md)) spreads this evenly across the project's working days to get a monthly need for comparison.
- `start_month`/`end_month`: `"YYYY-MM"` strings, month granularity only — no day component, consistent with the monthly-only granularity of `[[assignment]]` and `data/working_days.toml`. TOML has no native partial-date type, so these are plain strings.
- `[[assignment]]` uses `year`/`month` integers (not a `"YYYY-MM"` string), for consistency with `data/working_days.toml` ([ADR 0005](0005-working-days-counts-only.md)). Project-level dates use the string form instead because they're a single range endpoint rather than a lookup key into another table.
- Assignment magnitude is **hours per month** (not a percentage), matching the unit used for `hours_total` and `member.hours_daily`.

## Alternatives considered

- **Two separate files** (projects, assignments): the original plan; rejected per explicit user preference for a single file.
- **Percentage-based assignments** (e.g. "50% in March"): not chosen — hours keeps the unit consistent across `hours_total`, `hours_daily`, and assignments, avoiding a percentage-to-hours conversion step.

## Consequences

- `data/planning.toml` mixes relatively stable data (`[[project]]`) with frequently-changing data (`[[assignment]]`) in one file. Acceptable given the manual, rough-estimate scope — revisit if the file becomes unwieldy to hand-edit.
- The tool is responsible for referential integrity: every `assignment.project_id` must resolve to a `project.id`, every `assignment.member_id` must resolve to a team member `id` (ADR 0002).
- Ground-truth files are now back to three total: `data/team_members.toml`, `data/planning.toml`, `data/working_days.toml`.
