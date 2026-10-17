# ADR 0015: Entity model shaped around the computations, not the file structure

- Status: accepted
- Date: 2026-10-11

## Context

The ground-truth TOML files (ADR 0001) nest assignments inside each project, keyed by month (ADR 0007), because that's the natural shape for hand-editing. The Java domain model that reads these files doesn't have to mirror that shape — it can instead be shaped around what the capacity/gap computations (ADR 0004, 0010) and the report rows (ADR 0014) actually need. We discussed both options directly; a model that mirrors the files would need the percentage-sum and skill-coverage logic scattered across `Project`/`TeamMember` themselves, where a model shaped around the computations can keep that logic in one place.

## Decision

Three layers:

1. **Loaded entities** — thin, close to the TOML files: `TeamMember`, `Project`, `WorkingDays`. The one deviation from the file structure: each project's nested per-month assignment lists are flattened into standalone `Assignment` objects (`member`, `project`, `month`, `percentage`) at load time. `Assignment` is the atomic fact everything downstream derives from, rather than staying nested inside `Project`.

2. **Computation layer** — the actual domain logic, operating over the loaded entities, answering exactly the questions the reports need:
   - a member's total assigned percentage in a given month, with a per-project breakdown and an over-allocation flag (ADR 0008);
   - a member's spare percentage for a month, paired with their skills (ADR 0004);
   - a project's skill coverage for a month — which of its `skills_needed` have any assigned percentage against them that month, and which don't (ADR 0010).

   This is also the intended seam for the future optimization mode (ADR 0004) — it would plug in here, producing/writing `Assignment`s, using the same loaded entities as input.

3. **Report rows** — the computation layer's output is already report-shaped (e.g. a per-member-per-month summary, a per-project-per-month summary): flat objects that the terminal and HTML renderers (ADR 0012) just format. No further derivation happens in the renderers.

Net effect: `TeamMember` and `Project` stay dumb data holders. All percentage-sum/coverage logic lives in the computation layer. Renderers do no math.

## Alternatives considered

- **File-shaped model** (entities mirror the TOML nesting, e.g. assignments stay nested inside `Project`, and `Project`/`TeamMember` expose their own capacity/coverage methods): rejected — scatters the computation logic across multiple entity classes instead of one layer, and makes the future optimizer's job (writing assignments back) less clean, since it would need to reach into nested per-project structures rather than producing flat `Assignment` facts.

## Consequences

- Loading a TOML file now includes a flattening step (nested per-project assignments → a flat list of `Assignment`s) rather than a direct 1:1 deserialization.
- The computation layer is the single place that implements ADR 0004/0008/0010's formulas; unit tests (ADR 0013) target this layer directly, independent of file parsing or rendering.
- Report rows produced by the computation layer should stay flat/simple per ADR 0014 — if a report needs a shape the computation layer doesn't produce, that's a sign to add a method there, not to let a renderer start deriving values itself.
