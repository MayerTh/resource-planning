# Resource Planning

A single-user tool for planning which team members are allocated to which
projects, and for visualizing spare capacity and staffing gaps. See
[`PROJECT.md`](PROJECT.md) for the full project overview and
[`docs/adr`](docs/adr) for the design decisions behind it.

There are two interfaces, sharing the same data files:

- a **CLI** that generates reports (terminal + HTML),
- a **web editor** for editing the data through a browser instead of hand-editing TOML.

## Data files

All planning data lives in hand-edited TOML files under [`data/`](data):

- `data/team_members.toml` — who's on the team, their daily hours, skills.
- `data/planning.toml` — projects and their month-by-month assignments.
- `data/working_days.toml` — working-day counts per month (static reference data).

Edit these directly in a text editor, or use the web editor below. They're
plain text and git-trackable, so you can diff/commit planning changes like
code.

The repo ships with example data to show the schema. Replace it with your
own team/projects when you're ready to use this for real — `data/` is a
regular tracked directory, not gitignored.

## Prerequisites

The Nix flake provides a dev shell with the JDK and Maven already set up:

```sh
nix develop
```

Without Nix, install JDK 21 and Maven yourself.

## CLI: generating reports

Build everything from the repo root:

```sh
mvn package
```

Run the CLI against `data/`:

```sh
java -jar cli/target/cli.jar [output.html]
```

This prints a report to the terminal and also writes an interactive HTML
report (default path `report.html`, or the path given as the first
argument) with sortable tables — open it in a browser.

By default the CLI reads from `data/` relative to the current directory.
To point it at a different directory (e.g. when running from elsewhere, or
in a container), set `RESOURCEPLANNING_DATA_DIR`:

```sh
RESOURCEPLANNING_DATA_DIR=/path/to/data java -jar cli/target/cli.jar
```

The report covers, per team member and month: assigned %, spare capacity,
over-allocation; and per project and month: covered/uncovered skills needed.

## Web editor: editing the data

The web editor is a [Quarkus](https://quarkus.io) app for editing
`team_members.toml` and `planning.toml` as tables in a browser, as a
friendlier alternative to hand-editing TOML. It's local-network-trusted only
— there's no authentication.

Run it in dev mode from the repo root:

```sh
mvn -pl web-editor quarkus:dev
```

Then open http://localhost:8080 in a browser. It reads/writes the same
`data/` directory as the CLI (configurable via the
`resourceplanning.data-dir` property, or `RESOURCEPLANNING_DATA_DIR` when
running the packaged container).

From there you can list/add/edit/delete team members and projects, and
add/remove assignments per project per month. Validation errors (e.g. an
assignment referencing an unknown team member) are reported back instead of
corrupting the files.

## Running tests

```sh
mvn test
```

Runs unit tests across all three modules (`core`, `cli`, `web-editor`),
backed by fixture TOML files.

## Typical workflow

1. Edit `data/team_members.toml` and `data/planning.toml` — either by hand
   or through the web editor — to reflect who's on the team and what
   they're assigned to.
2. Run the CLI to see spare capacity and staffing gaps, as text in the
   terminal or as a browsable HTML report.
3. Adjust assignments based on what the report shows, and repeat.

Auto-optimization (the tool proposing assignments itself) is explicitly out
of scope for now — planning is done manually, with the tool only
visualizing the result.
