# ADR 0009: Confirm Java + Maven + Nix as the implementation stack

- Status: accepted
- Date: 2026-09-23

## Context

The repo was originally scaffolded as a Java/Maven project with a Nix flake for the dev environment, before any planning for this tool happened. Whether to keep that stack or switch (e.g. to Python, given this is a data/CLI/reporting tool) was an open question since early in the planning discussion.

## Decision

Stay with **Java + Maven + Nix**.

## Alternatives considered

- **Python**: faster to iterate for a CLI + data + charts tool, but would discard the existing scaffold; not chosen.

## Consequences

- The entity model, TOML parsing (`tomlj`, per [ADR 0001](0001-toml-ground-truth-files.md)), and CLI implementation are all Java.
- The Nix flake continues to define the dev environment/JDK.
