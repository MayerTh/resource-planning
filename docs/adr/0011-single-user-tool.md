# ADR 0011: Single-user, local tool — no concurrent-access support

- Status: accepted
- Date: 2026-10-11

## Context

The ground-truth data lives in hand-edited TOML files (ADR 0001) in a git repo, read/written by a local CLI. Whether more than one person (e.g. multiple team leads or PMs) edits this data concurrently changes requirements around file locking, merge conflicts, and whether the tool needs any notion of "who changed what."

## Decision

**Single user, local tool.** Only the head of department edits the data and runs the CLI, on their own machine. No concurrent-access support (locking, conflict detection, multi-user accounts) is built. If multiple people ever need to edit the files, git itself (branches/merges) is the assumed coordination mechanism, same as any other hand-edited text file in a repo — no tool-level support beyond that.

## Alternatives considered

- **Shared/multi-user tool**: rejected — not the actual use case, and would add real complexity (locking or merge-aware editing, user accounts/permissions) for no current benefit.

## Consequences

- No locking, no "last writer wins" protection, no audit/change-attribution features need to be designed.
- If this changes later, revisit — likely means layering a sync/lock mechanism on top of the existing file format rather than redesigning it.
