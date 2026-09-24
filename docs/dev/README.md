# Developer docs

Index of `docs/dev/`. What each directory is, and whether it is still being written back — doc-writing rules (fix in place, ID scheme, language) are in [../README.md](../README.md).

| Path | What it is | Status |
|---|---|---|
| [conventions/](conventions/README.md) | Engineering conventions & facts — the detailed memory behind `AGENTS.md`: flavors, signing, toolchains, architecture, code rules, localization, workflow | **living** — describes the present; drift is a bug to fix in place |
| [architecture-review-cn/](architecture-review-cn/README.md) | Review of the View-era architecture; its findings (`AR-xx`, `C`/`R`/`A`) still feed the backlog | **record + backlog** — re-verify a finding against current code before acting on it; the View-era descriptions are history, don't back-fill today's code into them |
| [quick-wins-cn/](quick-wins-cn/README.md) | Small, independently shippable fixes distilled from that review (`QW-x`) | same as the review it comes from |
| [compose-migration-plan-cn/](compose-migration-plan-cn/README.md) | The View → Compose migration behind this branch, written as a teaching walkthrough (per-step plan, before/after code, API notes) — read its key decisions before touching UI | **frozen** — steps 0–7 all landed; its text and its chapter-opening correction blocks stay exactly as they were written |
| [JOTTINGS.md](JOTTINGS.md) | Unsorted personal notes | scratch |

A **frozen** doc keeps its own declaration in its README; the code and [conventions/](conventions/README.md) are the truth about the present. To add a directory here, add a row and say which of the three statuses it has.

## Reading order

1. [conventions/](conventions/README.md) — how this repo is built and changed
2. [architecture-review-cn/](architecture-review-cn/README.md) — the gap between code and target
3. [quick-wins-cn/](quick-wins-cn/README.md) — the small fixes carved out of that review
4. [compose-migration-plan-cn/](compose-migration-plan-cn/README.md) — only before touching UI on this branch; start with its key decisions
5. [JOTTINGS.md](JOTTINGS.md) — unsorted material
