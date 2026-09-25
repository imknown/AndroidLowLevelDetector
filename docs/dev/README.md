# Developer docs

Index of `docs/dev/`. What each directory is, and whether it is still being written back — doc-writing rules (fix in place, ID scheme, language) are in [../README.md](../README.md).

| Path | What it is | Status |
|---|---|---|
| [conventions/](conventions/README.md) | Engineering conventions & facts — the detailed memory behind `AGENTS.md`: flavors, signing, toolchains, architecture, code rules, localization, workflow | **living** — describes the present; drift is a bug to fix in place |
| [issues-cn/](issues-cn/README.md) | Project issues summary: the View-era architecture review's findings (`AR-xx`, `C`/`R`/`A`), the quick-win fixes (`QW-x`), the Compose-migration code reviews (`F-x`), and the 2026-09-25 full-repo sweep findings (`N1`~`N16`) all feed the backlog | **record + backlog** — re-verify a finding against current code before acting on it; the View-era descriptions are history, don't back-fill today's code into them |
| [JOTTINGS.md](JOTTINGS.md) | Unsorted personal notes | scratch |

A **frozen** doc keeps its own declaration in its README; the code and [conventions/](conventions/README.md) are the truth about the present. To add a directory here, add a row and say which of the three statuses it has.

## Reading order

1. [conventions/](conventions/README.md) — how this repo is built and changed
2. [issues-cn/](issues-cn/README.md) — the gap between code and target: architecture findings, quick-win fixes (deferred), and the Compose-migration reviews
3. [JOTTINGS.md](JOTTINGS.md) — unsorted material
