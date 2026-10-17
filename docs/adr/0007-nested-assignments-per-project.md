# ADR 0007: Nest assignments under their project, keyed by month

- Status: accepted (nesting structure still applies; `hours` field superseded by `percentage`, see [ADR 0008](0008-percentage-based-assignments.md))
- Date: 2026-09-23
- Supersedes: the assignment-storage part of [ADR 0006](0006-single-planning-file.md)

## Context

ADR 0006's flat `[[assignment]]` array-of-tables (each row carrying `project_id` and `member_id` as foreign keys) is not intuitive to hand-edit — to see who's on a project in a given month, you have to scan/filter a separate flat list instead of reading it directly under the project.

## Decision

Assignments are nested under their `[[project]]` entry, in a `project.assignments` sub-table keyed by month (`"YYYY-MM"`), each mapping to a list of `{ member, hours }` entries:

```toml
[[project]]
id = "internal-tools-revamp"
name = "Internal Tools Revamp"
start_month = "2026-01"
end_month = "2026-06"
hours_total = 800
skills_needed = ["java", "typescript"]
responsible = "Markus Weber"

  [project.assignments]
  "2026-01" = [ { member = "anna.mayer", hours = 40 } ]
  "2026-02" = [ { member = "anna.mayer", hours = 40 } ]
  "2026-03" = [ { member = "anna.mayer", hours = 40 } ]

[[project]]
id = "data-platform-migration"
name = "Data Platform Migration"
start_month = "2026-03"
end_month = "2026-12"
hours_total = 1400
skills_needed = ["python", "sql"]
responsible = "Lea Fischer"

  [project.assignments]
  "2026-03" = [
    { member = "lea.fischer", hours = 100 },
    { member = "jonas.schmidt", hours = 60 },
  ]
  "2026-05" = [ { member = "jonas.schmidt", hours = 160 } ]
```

This is valid TOML: `[project.assignments]` after `[[project]]` defines a sub-table of the *last* array element (the same pattern as TOML's own canonical `[[fruits]] ... [fruits.physical]` example).

**Why a list of `{ member, hours }` objects instead of `member_id = hours` pairs**: team-member ids are `firstname.lastname` slugs (ADR 0002), and TOML treats dots inside a *key* as a nested-table path — `anna.mayer = 40` would parse as table `anna` containing key `mayer`, not the literal key `"anna.mayer"`. Quoting (`"anna.mayer" = 40`) would work but is an easy mistake to make/forget by hand. Using `member` as a plain string *value* instead of a key sidesteps the issue entirely, at the cost of slightly more verbose entries.

## Alternatives considered

- **Keep the flat `[[assignment]]` list** (ADR 0006 original): rejected — not intuitive to hand-edit, the stated motivation for this change.
- **`"anna.mayer" = 40` style, quoting member ids as keys**: more compact, but fragile to hand-edit (forgetting the quotes silently changes the data structure rather than erroring loudly).

## Consequences

- Referential integrity check changes shape: the tool now walks each project's `assignments` sub-table rather than joining a flat list on `project_id`; `member` values still must resolve to a `data/team_members.toml` id.
- `project_id` is no longer needed on assignment entries — membership in a project is now structural (nested under it) rather than a foreign key.
- Everything else from ADR 0006 (single file, project fields, monthly-only granularity, hours as the unit) is unchanged.
