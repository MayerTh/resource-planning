# ADR 0002: Human-readable slug as the stable team-member ID

- Status: accepted
- Date: 2026-09-23

## Context

Team members need a stable identifier so that other files (projects, assignments) can reference them without breaking if e.g. a last name is corrected. At the same time, the head of department wants to be able to look at the projects/assignments file and immediately see *who* is referenced, without a separate lookup step into the team-members file.

A plain opaque id (e.g. a UUID or incrementing number) would be stable but unreadable. Referencing people by raw `first_name`/`last_name` would be readable but not a good key (not guaranteed unique, and would require updating every reference if a name is corrected).

## Decision

Team members get a stable `id` field that is itself a **human-readable slug** in the form `firstname.lastname`, lowercase, e.g. `anna.mayer`. This id is what other files use to reference the person — no separate name lookup needed.

```toml
[[member]]
id = "anna.mayer"
first_name = "Anna"
last_name = "Mayer"
hours_daily = 8
pay_schema = "d4"
skills = ["java", "python", "typescript"]
```

## Alternatives considered

- **Opaque id (UUID/int)** + separate name fields: maximally stable, but references in the projects/assignments files would be unreadable without cross-referencing the team-members file.
- **Initial + lastname** (e.g. `amayer`): shorter, but less immediately readable than `firstname.lastname`.
- **Raw name as key**: readable, but not a good key — collisions possible, and any name correction would require updating every reference.

## Consequences

- Collisions (two people with the same `firstname.lastname`) must be handled manually by disambiguating the slug (e.g. appending a digit) — acceptable given the expected team size and manual editing workflow.
- If someone's name changes, the `id` slug is **not** automatically updated (that would break references); the id is treated as a stable key independent of a later name correction, even if it drifts from the current name.
- The tool should validate that `id` values are unique and that references in other files resolve to an existing `id`.
