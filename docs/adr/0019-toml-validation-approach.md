# ADR 0019: TOML validation — structural + referential checks, collect-all-errors reporting

- Status: accepted
- Date: 2026-10-11
- Fills in the "validation schema" item from [PROJECT.md](../../PROJECT.md)'s architecture direction (first raised alongside [ADR 0015](0015-computation-shaped-entity-model.md))

## Context

The three ground-truth TOML files (ADR 0001) are hand-edited, so malformed or inconsistent entries (a typo'd member id in an assignment, an invalid `pay_schema`, a missing field) need to be caught at load time with a clear message, rather than surfacing as a confusing `NullPointerException` or silently-wrong report deep in the computation layer.

## Decision

Validation happens in `TomlDataLoader` (`core`), during the same pass that builds the layer-1 entities (ADR 0015), and checks:

- **Required fields** are present and non-blank/non-null for every record in all three files (e.g. `id`, `first_name`, `hours_daily`, `skills_needed`, ...).
- **Enum values**: `pay_schema` must be one of `d3`/`d4`/`d5`.
- **Format**: `start_month`/`end_month`/assignment month keys must parse as `YYYY-MM`; `start_month` must not be after `end_month`.
- **Referential integrity**: an assignment's `member` must match an existing `team_members.toml` id. (`project.responsible` is deliberately *not* checked — ADR notes it's free text, not necessarily a team member.)
- **Uniqueness**: `team_members.toml` ids and `planning.toml` project ids must be unique; `working_days.toml` must not have duplicate `(year, month)` entries.
- **Basic sanity bounds**: `working_days.toml` month must be 1-12; counts and percentages must not be negative. (Percentages *over* 100 are valid and intentional — that's over-allocation, reported not rejected, per ADR 0008/0010.)

**All errors are collected, not fail-fast.** A single `DataValidationException` is thrown carrying every error found across all three files in one pass (file + row + field context in each message), so a hand-edit session surfaces every problem at once instead of a fix-rerun-fix loop one error at a time. TOML syntax errors (unparseable files) are collected the same way and also stop the load, since no semantic validation is possible on an unparseable file.

Invalid or incomplete entries are simply excluded from the resulting entities rather than causing a partial/corrupt object — but since any validation error throws before the data is returned, this only matters for how the loader accumulates errors internally, not for callers.

## Alternatives considered

- **Fail-fast (throw on the first error)**: rejected — would turn a multi-mistake hand-edit into a slow one-error-at-a-time loop, which matters more for a hand-edited file than for machine-generated input.
- **Schema-description library/JSON-Schema-for-TOML**: rejected — the checks needed (field presence, enum membership, cross-file id references) are simple enough that a declarative schema library would add a dependency and indirection without reducing the amount of code meaningfully; plain validation code stays more directly readable (per the project's human-readability direction).

## Consequences

- `TomlDataLoader.load(...)` either returns fully-valid `PlanningData` or throws `DataValidationException` with the complete error list — callers (CLI, and later `web-editor`) never see partially-valid data.
- Adding a new required field or constraint later means adding one check in `TomlDataLoader`, following the same `requireX(...)`-collects-into-`errors` pattern already established.
- Covered by `TomlDataLoaderValidationTest` (ADR 0013), which asserts the full multi-error collection behavior against a fixture with one mistake of each kind.
