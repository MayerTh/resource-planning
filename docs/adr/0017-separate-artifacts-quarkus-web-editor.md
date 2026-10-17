# ADR 0017: Separate CLI/web-editor artifacts via Maven multi-module; Quarkus for the web editor

- Status: accepted
- Date: 2026-10-11
- Refines [ADR 0016](0016-web-editor-second-interface.md); keeps [ADR 0009](0009-confirm-java-maven-nix-stack.md)'s stack for the CLI

## Context

ADR 0016 added a web editor as a second interface alongside the CLI. The default option discussed was a single dual-mode jar (`report` vs. `serve`). The user instead wants the CLI and the web editor to ship as genuinely separate artifacts/images, and wants to use Quarkus for the web editor specifically, based on existing familiarity with it.

## Decision

Restructure the repo as a **Maven multi-module project**:

- **`core`** — the shared domain model, TOML loader/validation, and computation layer (ADR 0015 layers 1 & 2). Plain Java, no framework dependencies. Consumed by both other modules.
- **`cli`** — the existing plain-Java CLI (ADR 0009), depends on `core`, owns reporting (terminal + HTML export, ADR 0012/0014). Packaged as a runnable jar.
- **`web-editor`** — a **Quarkus** application, depends on `core`, owns editing of `team_members.toml`/`planning.toml` with table views (ADR 0016).

Quarkus is used **only** for `web-editor` — `core` and `cli` stay framework-free plain Java, consistent with ADR 0009 and the project's "simple/human-readable" direction; no CDI/DI machinery in the parts that don't need a web server. The web editor's pages are **server-rendered** (Qute templates) rather than a separate frontend build — in line with ADR 0014's "flat, simple" philosophy extended to the editor's table views; no SPA/JS build step.

Containerization is handled **per module**: `web-editor` uses Quarkus's built-in container-image tooling (the standard generated `src/main/docker/Dockerfile.jvm`, etc.); `cli` gets its own small, plain Dockerfile (JRE base image running the jar). Two artifacts, two images.

## Alternatives considered

- **Single dual-mode jar** (one `Main` dispatching `report`/`serve`): rejected per explicit preference for separate artifacts — also keeps the CLI free of Quarkus's dependency/startup footprint.
- **Plain embedded server (e.g. Javalin) for the web editor**: rejected — Quarkus was explicitly requested, and its built-in container-image tooling directly serves the Docker-packaging goal with less custom setup.

## Consequences

- The repo moves from a single-module Maven project to a parent POM with 3 modules; the current `pom.xml`/`src/main/java/org/resourceplanning` content becomes the starting point for `core` and `cli`.
- Two Dockerfiles/images to maintain instead of one, one per artifact.
- `core` must stay dependency-light (no Quarkus/CDI annotations) so it remains usable from the plain-Java CLI without dragging in web-framework concerns.
- Quarkus's own testing/dev-mode tooling can be used for `web-editor`'s tests; `core`/`cli` keep the plain JUnit setup from [ADR 0013](0013-testing-strategy.md).
