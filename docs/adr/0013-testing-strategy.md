# ADR 0013: Unit tests plus a fixed fixture dataset, run after every major change

- Status: accepted
- Date: 2026-10-11

## Context

We're about to implement the entity model and the capacity/gap computation logic (ADR 0004, 0010). That logic has already gone through several schema revisions in planning, and is exactly the kind of code where a small change (e.g. how a percentage or working-day count is read) can silently break a calculation. We need a way to catch regressions as the code evolves, not just verify correctness once at the end.

## Decision

- **Unit tests are required** for the capacity/gap computation logic and the TOML parsing/validation layer. A change isn't done until its tests pass.
- Maintain a **small, fixed set of fixture TOML files** under `src/test/resources` (decoupled from `data/`, which holds the user's own real/example ground truth and will eventually contain real, possibly sensitive data). Fixtures encode known scenarios — the same kind already sketched informally in `data/`'s comments (an under-staffed project, an over-allocated member, a member with spare capacity) — each paired with a hand-verified expected result.
- This fixture-backed test suite is **run after every major change** (new feature, schema change, refactor) as part of the normal dev loop (`mvn test`), not just once before calling a feature finished.

## Alternatives considered

- **Manual re-testing only** (re-run the CLI against `data/` and eyeball the output): rejected — doesn't scale as scenarios grow, and `data/` will eventually hold real data rather than clean, known edge cases.
- **Generated/property-based test data**: rejected for now — the scenarios worth testing are specific known edge cases (over-allocation, under-staffing), better hand-picked than randomly generated.

## Consequences

- Need a `src/test/resources` fixture set from the start, separate from `data/`.
- The current example data in `data/*.toml` (originally written to validate the schema shape) can be ported into the fixture set as a starting point; `data/` itself can then be replaced with real data without touching the tests.
- Running the test suite becomes a routine step after any major change, not a final checkbox.
