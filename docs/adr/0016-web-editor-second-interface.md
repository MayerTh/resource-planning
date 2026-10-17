# ADR 0016: Local web UI for editing `team_members.toml`/`planning.toml` — a second interface alongside the CLI

- Status: accepted
- Date: 2026-10-11
- Revises the "No GUI" non-goal from [PROJECT.md](../../PROJECT.md) (originally set alongside [ADR 0004](0004-manual-planning-with-gap-visualization.md))

## Context

Hand-editing TOML in a text editor works, but `planning.toml` in particular changes often (new assignments every month), and a table-based web UI is friendlier for that than raw TOML. We discussed scope directly: which files it edits, how it relates to the existing CLI/report pipeline, and how "single-user" (ADR 0011) interacts with wanting it reachable from other devices.

## Decision

- **Scope**: the website edits `data/team_members.toml` and `data/planning.toml` only. `data/working_days.toml` stays out of scope — it's static, generated-once reference data not meant for regular hand-editing (ADR 0005).
- **Second interface, not a replacement**: the website is an additional interface over the same entity model (ADR 0015). The CLI keeps doing the reporting job — terminal output and the HTML report export (ADR 0012, 0014) are unchanged. The website's own table views show the *raw* data (members, projects, assignments) to make editing easier; it is not a second reporting/dashboard surface, avoiding two parallel report-rendering paths.
- **Single source of truth stays the TOML files on disk.** The website reads and writes them directly — no database, no separate cache/state. A website edit and a hand edit are indistinguishable to the rest of the system (and to git).
- **Reachable on the local network, not just localhost** — convenient for editing from another device at home/office. This does *not* change ADR 0011: still conceptually single-user, no accounts, no concurrent-write protection. The trust boundary is "whoever is on this local network," not a public service — no authentication is in scope. This should not be exposed beyond a trusted local network.

## Alternatives considered

- **Website also takes over reporting** (duplicating ADR 0012's HTML export): rejected — would mean building and maintaining two HTML-rendering paths for the same underlying data; the CLI already owns that job.
- **Keep editing TOML-only, no website**: rejected per explicit request — a table UI is meaningfully friendlier for frequent edits than hand-editing TOML.
- **Multi-user/authenticated website**: rejected — out of scope for a single-user tool (ADR 0011); adding accounts/auth would be disproportionate complexity.

## Consequences

- PROJECT.md's "No GUI" non-goal needs updating: it now applies only to the *reporting* job, which stays CLI-only. Editing gets a GUI.
- Need a small web server component (framework choice is a separate decision) with read/write access to `team_members.toml` and `planning.toml`, reusing the same loader/validation (ADR 0013's validation direction) and layer-1 entities (ADR 0015) as the CLI.
- Because there's no authentication, running this on an untrusted network is explicitly out of scope — documented here so it isn't accidentally exposed later without re-examining this decision.
