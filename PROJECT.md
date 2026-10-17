# Resource Planning — Project Overview

_Last updated: 2026-10-11_

## What this project is

A CLI tool for a head of software department to plan team members onto projects, used by a single person, run locally or on their local network (see [ADR 0011](docs/adr/0011-single-user-tool.md)). The CLI reads/writes plain-text ground-truth files and renders reports both inline in the terminal and as an interactive HTML file (see [ADR 0012](docs/adr/0012-dual-chart-output.md)). A small local web app is a second interface for *editing* `team_members.toml`/`planning.toml` with table views, reading/writing the same files (see [ADR 0016](docs/adr/0016-web-editor-second-interface.md)), and also shows the same HTML report the CLI produces (see [ADR 0020](docs/adr/0020-web-editor-also-serves-reports.md)). Planning is done manually by the user; the tool computes and visualizes spare capacity and staffing gaps (see [ADR 0004](docs/adr/0004-manual-planning-with-gap-visualization.md), [ADR 0010](docs/adr/0010-percentage-based-gap-reporting.md)). Auto-optimization (the tool doing the assignment itself) is a deferred future extension, not part of the initial scope.

## Ground truth data

Three hand-edited files act as the source of truth (format: TOML, see [ADR 0001](docs/adr/0001-toml-ground-truth-files.md)):

1. **`data/team_members.toml`** — `id` (stable, human-readable slug `firstname.lastname`, e.g. `anna.mayer`, see [ADR 0002](docs/adr/0002-slug-id-for-team-members.md)), `first_name`, `last_name`, `team` (string), `role` (string), `hours_daily`, `pay_schema` (enum: `d3`/`d4`/`d5`), `skills` (list of programming languages).
2. **`data/planning.toml`** — projects and their monthly assignments, combined in one file (see [ADR 0006](docs/adr/0006-single-planning-file.md), [ADR 0007](docs/adr/0007-nested-assignments-per-project.md), [ADR 0008](docs/adr/0008-percentage-based-assignments.md)):
   - `[[project]]`: `id` (kebab-case slug of the name), `name`, `start_month`/`end_month` (`"YYYY-MM"` strings, month granularity only), `hours_total` (total hour budget for the whole project — **reference value only**, not read by the gap computation, see [ADR 0010](docs/adr/0010-percentage-based-gap-reporting.md)), `skills_needed` (list), `responsible` (free-text name, not necessarily a team member).
   - `project.assignments`: a sub-table nested under each project, keyed by month (`"YYYY-MM"`), each mapping to a list of `{ member, percentage }` entries — `percentage` is relative to that member's own capacity that month, not a flat FTE reference. No separate flat assignment list.
3. **`data/working_days.toml`** — static, generated-once reference data: number of working days per month for Bavaria, Germany, 2026–2028 (count only, not full date lists — see [ADR 0005](docs/adr/0005-working-days-counts-only.md), superseding [ADR 0003](docs/adr/0003-calendar-reference-file.md)). Unlike the other two, this isn't meant to be hand-edited regularly.

All three files currently contain example data (3 team members, 2 projects, a handful of assignments) to validate the schema — replace with real data when ready.

Over-/under-allocation are reported in the tool's output only, never blocking; personal vacation/PTO is explicitly out of scope (capacity = working days × `hours_daily` only) — see [ADR 0004](docs/adr/0004-manual-planning-with-gap-visualization.md).

## Goals

- Plan/track which team members are allocated to which projects.
- Keep data in human-editable files, git-trackable.
- Visualize spare/unplanned capacity per team member and staffing gaps per project.
- Produce charts/tables from the CLI for an overview of allocations.
- Offer a local web UI for editing `team_members.toml`/`planning.toml` as tables, as a friendlier alternative to hand-editing TOML (see [ADR 0016](docs/adr/0016-web-editor-second-interface.md)), and for viewing the same report there (see [ADR 0020](docs/adr/0020-web-editor-also-serves-reports.md)).
- Leave room for a future auto-optimization planning mode, without changing file formats.

## Non-goals

- No automatic assignment/optimization in the initial version (deferred, see [ADR 0004](docs/adr/0004-manual-planning-with-gap-visualization.md)).
- No authentication/multi-user accounts on the web editor — local-network-trusted only (see [ADR 0016](docs/adr/0016-web-editor-second-interface.md)).

## Decisions

Significant architecture/design decisions live in [docs/adr](docs/adr) as individual Architecture Decision Records. This section lists them chronologically:

| ADR | Title | Status |
|-----|-------|--------|
| [0001](docs/adr/0001-toml-ground-truth-files.md) | Use TOML for the ground-truth data files | accepted |
| [0002](docs/adr/0002-slug-id-for-team-members.md) | Human-readable slug as the stable team-member ID | accepted |
| [0003](docs/adr/0003-calendar-reference-file.md) | Static calendar reference file for Bavarian working days | superseded by 0005 |
| [0004](docs/adr/0004-manual-planning-with-gap-visualization.md) | Manual assignment planning, with computed gap/spare-capacity visualization; optimization deferred | accepted |
| [0005](docs/adr/0005-working-days-counts-only.md) | Working-days file stores counts only, renamed to `working_days.toml` | accepted |
| [0006](docs/adr/0006-single-planning-file.md) | Single `planning.toml` file for projects and assignments, monthly granularity | partially superseded by 0007, 0008 |
| [0007](docs/adr/0007-nested-assignments-per-project.md) | Nest assignments under their project, keyed by month | accepted (magnitude field superseded by 0008) |
| [0008](docs/adr/0008-percentage-based-assignments.md) | Assignment magnitude is a percentage of the member's own capacity, not hours | accepted |
| [0009](docs/adr/0009-confirm-java-maven-nix-stack.md) | Confirm Java + Maven + Nix as the implementation stack | accepted (Nix's packaging role narrowed by 0018) |
| [0010](docs/adr/0010-percentage-based-gap-reporting.md) | Staffing-gap reporting stays percentage/skill-based; `hours_total` stays reference-only | accepted |
| [0011](docs/adr/0011-single-user-tool.md) | Single-user, local tool — no concurrent-access support | accepted |
| [0012](docs/adr/0012-dual-chart-output.md) | Reports render both inline-terminal and as an interactive HTML file | accepted |
| [0013](docs/adr/0013-testing-strategy.md) | Unit tests plus a fixed fixture dataset, run after every major change | accepted |
| [0014](docs/adr/0014-minimal-report-scope.md) | Keep the set of reports small, with simple/flat structure | accepted |
| [0015](docs/adr/0015-computation-shaped-entity-model.md) | Entity model shaped around the computations, not the file structure | accepted |
| [0016](docs/adr/0016-web-editor-second-interface.md) | Local web UI for editing `team_members.toml`/`planning.toml` — a second interface alongside the CLI | accepted (reporting alternative superseded by 0020) |
| [0017](docs/adr/0017-separate-artifacts-quarkus-web-editor.md) | Separate CLI/web-editor artifacts via Maven multi-module; Quarkus for the web editor | accepted |
| [0018](docs/adr/0018-nix-devshell-only.md) | Nix flake scoped to dev-shell only; Docker is the packaging/distribution mechanism | accepted |
| [0019](docs/adr/0019-toml-validation-approach.md) | TOML validation — structural + referential checks, collect-all-errors reporting | accepted |
| [0020](docs/adr/0020-web-editor-also-serves-reports.md) | Web editor also serves the HTML report, reusing the CLI's renderer | accepted |

## Architecture

- **Modules** (see [ADR 0017](docs/adr/0017-separate-artifacts-quarkus-web-editor.md)): a Maven multi-module project —
  - `core` — domain model + loader/validation + computation layer + HTML report rendering, plain Java, no framework deps. **Implemented and tested.**
  - `cli` — plain-Java CLI (ADR 0009), depends on `core`, owns the terminal report view and writes the HTML report to a file. **Implemented and tested.**
  - `web-editor` — Quarkus app, depends on `core`, owns editing of `team_members.toml`/`planning.toml` (ADR 0016), server-rendered (Qute) table views, plus a `/reports` page serving the same HTML report live (ADR 0020), packaged/containerized via Quarkus's standard tooling. **Implemented and tested.**
- **Entity model** (see [ADR 0015](docs/adr/0015-computation-shaped-entity-model.md), in `core`, package `org.resourceplanning.domain`): `TeamMember`, `Project`, `Assignment` (flattened out of the TOML's per-project nesting), `WorkingDaysCalendar`, bundled as `PlanningData`.
- **Loader + validation** (`org.resourceplanning.io.TomlDataLoader`, using the `tomlj` library): parses the three TOML files into `PlanningData`, or throws `DataValidationException` with every error found in one pass — see [ADR 0019](docs/adr/0019-toml-validation-approach.md) for exactly what's checked.
- **Writer** (`org.resourceplanning.io.TomlDataWriter`): the persistence counterpart used by `web-editor` — serializes `TeamMember`/`Project`/`Assignment` lists back to the same TOML layout (round-trips through `TomlDataLoader`, see `TomlDataWriterTest`).
- **Computation layer** (`org.resourceplanning.compute.CapacityCalculator`, in `core`): produces `MemberMonthSummary` (assigned/spare %, over-allocation flag, per-project breakdown) and `ProjectMonthSummary` (covered/uncovered `skills_needed`) — these *are* the report rows, per ADR 0015; this is also the intended seam for a future optimizer.
- **Renderers**: `org.resourceplanning.report.HtmlReportRenderer` (sortable HTML tables, vanilla JS, no build step) lives in `core`, shared by both `cli` and `web-editor` (ADR 0020); `org.resourceplanning.report.TerminalReportRenderer` (plain text) stays in `cli` only. Both just format the computation layer's output, no math. Kept flat/simple per [ADR 0014](docs/adr/0014-minimal-report-scope.md).
- **`web-editor` pages** (`org.resourceplanning.webeditor`): `HomeResource`, `MembersResource`, `ProjectsResource` — list/add/edit/delete for team members and projects, plus per-project assignment add/delete, all via plain HTML forms over Qute templates (`src/main/resources/templates`); `ReportsResource` serves `/reports` by calling `CapacityCalculator` + `HtmlReportRenderer` directly, returning the same HTML document the CLI exports (ADR 0020). `PlanningRepository` resolves the data-file paths (`resourceplanning.data-dir` config property, default `../data`, overridable via `RESOURCEPLANNING_DATA_DIR` env var for containers) and wraps the loader/writer. `DataValidationExceptionMapper` turns a failed validation into a readable 400 response instead of a stack trace.
- The domain model stays decoupled from file-based persistence so the storage layer could be swapped later without touching the rest of the code.
- Favor **human-understandable naming** throughout (classes, fields) — readability over brevity/cleverness.
- **Testing**: unit tests over the entity/computation/io layers, backed by fixture TOML files under `core/src/test/resources/fixtures` (`valid`/`invalid`), run after every major change (see [ADR 0013](docs/adr/0013-testing-strategy.md)). `web-editor` uses `@QuarkusTest` + REST-assured against its own fixtures in `web-editor/src/test/resources/fixtures`; `core`/`cli` use plain JUnit 5. 17 tests total across the reactor, all green (`mvn test` from the repo root).
- **Containerization**: `cli/src/main/docker/Dockerfile` (plain `eclipse-temurin:21-jre`) and `web-editor/src/main/docker/Dockerfile.jvm` (Quarkus-generated UBI image) — both read `RESOURCEPLANNING_DATA_DIR` (default `/data`) and declare `/data` as a `VOLUME`, never baking `data/` into the image (ADR 0016). Not yet build-tested with an actual `docker build` (no Docker daemon available while implementing) — the jar/quarkus-app output locations the `COPY` instructions expect were verified to match after `mvn package`, but running the real `docker build` is still worth doing once you have Docker available.
- **Nix**: `flake.nix` provides a `devShell` only (JDK + Maven) — no `nix build`/`nix run` package output. Docker is the actual packaging/distribution mechanism for both artifacts (see [ADR 0018](docs/adr/0018-nix-devshell-only.md)).
- **`web-editor` frontend**: server-rendered via Quarkus's Qute templates (`quarkus-rest-qute` extension wires `TemplateInstance` as a JAX-RS response entity) — plain HTML forms/tables, no SPA/JS framework, no separate Node.js build step (ADR 0017).

## Still to do

- Verify the two Dockerfiles with an actual `docker build`/`docker run` (not possible in the environment this was implemented in).
- `data/` still holds example data (ADR 0001 era) — replace with real data when ready.
- Possible future polish, not required by any ADR: inline validation error display on the web-editor's forms (currently a plain-text 400 page), CSS/UX pass.

