# Documentation

Developer docs for AndroidLowLevelDetector. Product introduction and download links are in the [root README](../README.md).

## Contents

| Path | What it is |
|---|---|
| [spec-cn/](spec-cn/README.md) | Product spec (Chinese) |
| [dev/architecture-review-cn/](dev/architecture-review-cn/README.md) | Architecture review (View-era) |
| [dev/quick-wins-cn/](dev/quick-wins-cn/README.md) | Quick-win fix list (View-era) |
| [dev/conventions/](dev/conventions/README.md) | Engineering conventions & facts — the detailed memory behind `AGENTS.md` |
| [dev/compose-migration-plan-cn/](dev/compose-migration-plan-cn/README.md) | View → Compose migration plan (Chinese) |
| [dev/JOTTINGS.md](dev/JOTTINGS.md) | Personal notes |

Each directory's README describes its own contents, background, and caveats — this file is only an index; when adding a new doc directory, add a row to this table.

## Reading order

1. [spec-cn](spec-cn/README.md) — what the app should be
2. [dev/architecture-review-cn](dev/architecture-review-cn/README.md) — the gap between code and target
3. [dev/compose-migration-plan-cn](dev/compose-migration-plan-cn/README.md) — the current migration branch's plan
4. [dev/JOTTINGS.md](dev/JOTTINGS.md) — unsorted material

## Conventions

- Every requirement and finding has an ID (`FR-x`/`AC` for requirements, `ADR-x` for architecture decisions, `AR-xx` and `C`/`R`/`A` for review findings, `Q-x` for open questions). IDs are shared across all docs — reference them by ID.
- English is the default language for documents; non-English directories are marked with a language suffix (Chinese: `-cn`). Any language may still be used inside a doc when there is a reason.
- No document (memory docs included) may contain sensitive information: privacy data, passwords, keys, certificates.
- Keep docs short; when one grows too long, split it along natural seams, keeping a README as the index page.
