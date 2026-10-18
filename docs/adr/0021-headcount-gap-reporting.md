# ADR 0021: Headcount-gap reporting reuses `hours_total`, reversing ADR 0010 for aggregate sizing only

- Status: accepted
- Date: 2026-10-18
- Partially reverses [ADR 0010](0010-percentage-based-gap-reporting.md); builds on [ADR 0004](0004-manual-planning-with-gap-visualization.md), [ADR 0008](0008-percentage-based-assignments.md)

## Context

Two goals for the reports surfaced that the existing percentage/skill-based gap view doesn't answer:

1. Make sure every team member has enough project work — nobody sits idle.
2. Know how many people need to be hired to fully cover all projects' demand.

Neither needs skill-level gap detail (`skills_needed` stays a plain list, not quantified per skill — that scope is explicitly not wanted). Both need an actual headcount number, company-wide and per-project, which the percentage-only view from ADR 0010 can't produce: percentages are relative to each member's own capacity, with no absolute unit to sum across people or compare against a hiring target.

`hours_total` is the only field in the schema that carries an absolute, summable quantity for a project's demand. ADR 0010 made it reference-only because spreading it evenly across time and converting percentages back to hours stacks two approximations on a number meant to drive skill-matching — risking false precision. That objection is specific to skill-matching; it doesn't block using `hours_total` for a coarser, explicitly-approximate headcount estimate.

## Decision

**Reuse `hours_total`, spread evenly across each project's own `start_month`–`end_month` range, to compute headcount-gap numbers** — reversing ADR 0010's "never read `hours_total`" rule, but only for this aggregate sizing use case, not for skill-matching (which stays percentage/skill-based, unchanged).

Define **hours per head per month** = average `hours_daily` across current team members (fallback `8.0` if the team is empty) × working days that month (from `working_days.toml`). This converts a month's hour demand into an equivalent headcount figure.

Two new report elements, both computed only for the current month and forward (past months are excluded from every report view — terminal, HTML export, and the web-editor `/reports` page):

- **Overall monthly headcount gap** (`MonthlyHeadcountGap`): company-wide demand (sum of active projects' evenly-spread `hours_total`) vs. total team supply (headcount × hours-per-head), independent of actual assignments. Answers "how many people to hire."
- **Per-project monthly headcount gap** (`ProjectHeadcountGap`): a project's demand vs. that project's actually-assigned hours (assignment percentages converted to hours via each assigned member's capacity). Answers "which project is short right now." Rows are flagged short-staffed when assigned hours fall short of demand.

A third element, a **member-utilization heatmap** (assigned % per member per month, color-coded), surfaces idle members directly from existing assignment data — no new reversal needed, since it's plain percentage data already computed per ADR 0008.

All three render in `HtmlReportRenderer`: an inline-SVG bar chart for the overall gap, a table for per-project gaps, a CSS heatmap table for utilization — consistent with the project's existing plain-HTML/light-JS, no-build-step rendering style (ADR 0012).

## Alternatives considered

- **Leave ADR 0010 as-is, don't compute headcount at all**: rejected — there's no other field in the schema that gives an absolute demand number, so without this reversal the two stated goals (idle-detection and hiring sizing) can't be answered at all.
- **Add a new quantified per-skill demand field to `planning.toml`** instead of reusing `hours_total`: rejected — explicitly out of scope; the user wants headcount numbers only, not skill-quantity detail, and it would be a bigger schema change for a narrower win.
- **Compute per-project gap the same way as the overall gap** (demand vs. total team supply, ignoring actual assignments): rejected — per-project needs to answer "is *this* project short," which requires comparing against what's actually assigned to it, not against the whole team's capacity.

## Consequences

- `hours_total` is no longer universally reference-only: it's read by `CapacityCalculator.overallHeadcountGaps()`/`projectHeadcountGaps()`, while skill-matching (`ProjectMonthSummary`) still ignores it, per ADR 0010's original reasoning.
- The even-spread-over-time approximation ADR 0010 flagged still applies here, but the risk it guarded against (false confidence in skill-matching precision) doesn't carry over — headcount gaps are presented as a rough sizing figure, not a precise skill-coverage claim.
- All report views (terminal, HTML, web-editor `/reports`) are now implicitly scoped to the current month forward; past months are dropped everywhere, which also fixed example-fixture tests rotting against the real clock (the web-editor integration test now asserts on section headings rather than fixture row content for this reason).
- `CapacityCalculator` gained filtered overloads (`memberMonthSummaries(YearMonth from)`, `projectMonthSummaries(YearMonth from)`) alongside the existing unfiltered no-arg versions, so this is additive, not a breaking signature change.
- If a reliable per-skill hours gap is wanted later, it still needs a different input than `hours_total` spread evenly (e.g. per-skill monthly targets) — not pursued now, consistent with ADR 0010.
