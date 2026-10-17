# ADR 0010: Staffing-gap reporting stays percentage/skill-based; `hours_total` stays reference-only

- Status: accepted
- Date: 2026-10-11
- Resolves the open question left by [ADR 0008](0008-percentage-based-assignments.md) and [ADR 0004](0004-manual-planning-with-gap-visualization.md)

## Context

ADR 0008 moved assignments from hours to percentage-of-member-capacity, which left open how (or whether) `project.hours_total` — a flat hour budget for the whole project — still feeds an automatic per-month staffing-gap calculation. Converting `hours_total` into a monthly figure (spread evenly across the project's working days) and comparing it against assigned percentage converted back into hours is possible, but stacks two approximations (even spread over time + percentage-of-individual-capacity-to-hours) on top of each other, and doesn't obviously produce a number worth trusting.

## Decision

**Staffing-gap reporting stays purely percentage/skill-based.** For a project in a given month, the report shows which `skills_needed` have assigned capacity and which don't (e.g. "no one with Java assigned in March"), using only the `assignments` percentages — no hours-based "needs N more people" figure is computed.

`hours_total` remains in the project schema as a **reference value only**, with no computation behind it. It is not read by the gap/report module.

## Alternatives considered

- **Hours-based gap** (spread `hours_total` evenly across working days, compare against hours derived from assigned percentages): rejected — the double approximation makes the resulting number look more precise than it is, risking false confidence in a "rough estimate" tool (per ADR 0004's stated precision level).
- **Drop `hours_total` from the schema entirely**: rejected — it's still useful as a human reference figure for the project's rough size, even without the tool computing against it.

## Consequences

- The capacity/gap module (ADR 0004) never reads `hours_total`; it only reads `skills_needed` and `assignments` percentages.
- `hours_total` has no validation tying it to the assignment data (no referential/consistency check needed between the two).
- If a reliable hours-based gap is wanted later, it would need a different input than `hours_total` spread-evenly (e.g. per-month staffing targets) — not pursued now.
