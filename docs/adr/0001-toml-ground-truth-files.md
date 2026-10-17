# ADR 0001: Use TOML for the ground-truth data files

- Status: accepted
- Date: 2026-09-23

## Context

The tool needs a small number of hand-edited files that act as the ground truth for the domain data (team members, projects, assignments). These files must be easy for a human (the head of department) to edit directly in a text editor, diff-friendly under git, and parseable from Java.

Candidates considered: YAML, JSON, TOML, CSV.

## Decision

Use **TOML** for all ground-truth data files.

Reasons:
- No significant whitespace, unlike YAML — fewer hand-editing mistakes.
- No implicit type coercion surprises (YAML's "Norway problem": unquoted `no`/`yes`/version-like strings being coerced to booleans/numbers).
- Array-of-tables syntax (`[[member]]`) maps directly onto "a list of records with a few fields each," which is the shape of all three files.
- Mature Java parsing support (e.g. `tomlj`), fitting the existing Java/Maven/Nix stack.

## Alternatives considered

- **YAML**: more compact for deep nesting, but this data isn't deeply nested; indentation sensitivity and implicit typing are a real hand-editing risk.
- **JSON**: not meant for hand-editing (no comments, strict trailing-comma/quoting rules).
- **CSV**: fine for pure flat tables, but doesn't comfortably support a list-valued field like `skills` per person.

## Consequences

- All three ground-truth files (team members, projects, assignments) will use TOML.
- The Java code needs a TOML parsing dependency (`tomlj`).
- Schema validation (required fields, enum checks like `pay_schema`) is the tool's responsibility, since TOML itself won't enforce it.
