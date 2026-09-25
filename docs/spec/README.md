# LowLevelDetector Specification (Requirements & Architecture)

| | |
|---|---|
| **Version** | 0.4 (draft for the owner's review). The date and source of each decision are marked at its place in the body (rulings on open questions and the roadmap in [03](03-nfr-release-and-open-questions.md), the item catalog in [05](05-detection-item-catalog.md), requirements in [02](02-functional-requirements.md)); this column does not restate the revision process — run `git log -- docs/spec` to see the change history |
| **Date** | 2026-09-23 |
| **Status** | Draft — on 2026-09-05 the original SRS and ADD documents were merged into this one specification. It states *what the app should be*; today's code does not necessarily do all of it |
| **Source** | Interviews with the owner + a function-by-function walkthrough (2026-09-05). Apart from the two explicitly verified functions (the call order of `detect()` / `collectModels()`), it was **deliberately** written without following the source code. **Precisely for that reason, factual statements such as "which properties the app reads" must be checked back against the code** — the property families in FR-2 and §1.2 were rewritten from the code, which is authoritative |

> **Single-document principle**: requirements and architecture are written in the same specification. Split into two documents, both have to be maintained at once, and after a long time the two accounts no longer agree. Requirements are tagged `FR-x` / `AC`; architecture is tagged `§` / `ADR`.
>
> This specification consolidates the 19 original English booklets into six parts (the booklets were deprecated on 2026-09-11; everything useful was carried over). For the gap analysis between code and specification and the implementation plan, see [../dev/issues-cn/](../dev/issues-cn/README.md).

## Contents

| Chapter | Contents |
|---|---|
| [01 — Product and users](01-product-and-users.md) | Purpose, scope, glossary, who it is for; background, positioning, what this app is good at, what it will and will not do, roadmap highlights, the two release variants and data collection |
| [02 — Functional requirements](02-functional-requirements.md) | FR-1 ... FR-17: what "done" means for each requirement (acceptance criteria; core features plus the app shell and UI) |
| [03 — Non-functional requirements, release, and open questions](03-nfr-release-and-open-questions.md) | NFRs, the release scope and roadmap list (the only list maintained), success criteria, the undecided questions Q1 ... Q10 |
| [04 — Architecture and decisions](04-architecture-and-decisions.md) | Architecture principles, current state, target architecture (layering / unidirectional data flow UDF / state / detection engine / data / modules / dependency injection DI), tech stack, ADRs, error handling, testing, migration, risks |
| [05 — Detection item catalog](05-detection-item-catalog.md) | Appendix A: the item list (with order) for the three screens Home / Others / Prop, plus the item template |
