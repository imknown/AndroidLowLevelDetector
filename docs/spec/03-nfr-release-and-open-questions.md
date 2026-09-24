# 03 — Non-functional requirements, release, and open questions

Part of the "[LowLevelDetector specification](README.md)".

## 1. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Compatibility | Target **minSdk 24 (Android 7.0)** — following AndroidX's floor. On old or vendor-modified systems, feature detection must degrade gracefully (e.g. show unknown), never crash. |
| NFR-2 | Privacy | Detection in both variants runs on the phone; **detection results are never uploaded**. Whether reference data sync (FR-7) uses the network is under user control (FR-8, sync only). Cached reference data lives only in the app's private directory. **Telemetry follows the variant (FR-10)**: the `firebase` variant collects crash reports and usage data via Firebase (on by default, no runtime switch today); the `foss` variant collects nothing. |
| NFR-3 | Performance | Detection runs asynchronously; the results screen shows up promptly and detection work never blocks the main thread. |
| NFR-4 | Reliability | State survives UI recreation and process death per the lazy-load / cache rules (FR-13); refresh neither deadlocks nor stacks (FR-5); failures are isolated to a single item (FR-6). |
| NFR-5 | Maintainability | The codebase must be **easy for both humans and AI assistants to navigate**: one source of truth for state (SSOT), one-way data flow (UDF), **kept together by feature** — a feature's logic is concentrated in one place and never scattered across distant modules. |
| NFR-6 | Legality and security | Only lawful, read-only means (FR-9); no evasion tricks (NG6). |
| NFR-7 | Readability | The tri-state model must be understandable to non-experts (the Explorer persona). The verdict dot (Home only) is the visual rendering of the tri-states (one indicator): green = best (fully supported / already latest / good state), amber = middle (partially supported / newer / average state), red = worst (unsupported / stale / poor state). **Unknown / undetectable usually falls into red too**, but which color each case gets follows that detection item's own semantics — no cross-item uniform mapping is imposed. |
| NFR-8 | Adaptivity | The UI uses **adaptive layouts**: phone, tablet, foldable, window resizing / multi-window all work. |

## 2. Release scope and roadmap list

**Only this official list is maintained** — the lists once spelled out in old Part 02 §2.6, Part 04 §4.3, and §8 of this chapter all point here and are not repeated elsewhere.

What ✅ means: **already implemented** — the code exists and the acceptance criteria were verified. A ✅ in the "backlog" or "roadmap" columns means *planned to go there*, not that it is done.

| ID | Capability | Current | Backlog | Roadmap |
|---|---|:---:|:---:|:---:|
| FR-1 | Feature support detection (tri-state results) | ✅ | | |
| FR-2 | System parameters (auxiliary layer) | ✅ | | |
| FR-13 | Auto-detect on entry, lazy loading, caching, restore (includes FR-4) | ✅ | | |
| FR-5 | Pull-to-refresh | ✅ | | |
| FR-6 | Per-item error isolation (**not implemented yet** — an erroring detection crashes the whole page, see finding C1 in the architecture review; to be added in batch 2) | | ✅ | |
| FR-7/16 | Server reference data sync + offline cache + version merge | ✅ | | |
| FR-8/17 | Network switch (sync only, off by default) | ✅ | | |
| FR-10 | Two variants (`foss` / `firebase`) | ✅ | | |
| FR-11/12/14/15 | App shell, detection screens and verdict dots (the ✅ here counts only FR-11/14/15 and FR-12's shell layout — FR-12's scrolling behavior and NavigationRail are scheduled for the Compose batch) | ✅ | ✅ | |
| BL-7 | Runtime telemetry switch (firebase) | | ✅ | |
| BL-1 | Per-item detail expansion — **not doing it for now**: list cards keep only the ripple feedback, with no click action | | ✅ | |
| BL-2 | Copy / share (**both**; timing and interaction to be discussed before work starts) | | ✅ | |
| BL-3 | Single-row refresh | | ✅ | |
| BL-8 | Task-stack launch protection for special flag combinations (FR-11 hardening) — **deferred until user feedback arrives** (accept the hazard until then) | | ✅ | |
| RM-1 | Export (JSON/TOML, in-app / adb; format: Q4) | | | ✅ |
| RM-2 | Cross-platform viewer (CMP) | | | ✅ |
| RM-3 | User-defined queries (Q8) | | | ✅ |
| RM-4 | External query surface, e.g. AppFunctions (Q8) | | | ✅ |
| RM-5 | Native AI interaction | | | ✅ |
| ~~Q3~~ | ~~Product naming decision~~ **resolved (2026-09-22), takes no roadmap slot** | | | |

The engineering migration stages (Compose → CMP → viewer → query opening) are in the migration plan of "[04 — Architecture and decisions](04-architecture-and-decisions.md)".

## 3. Success criteria (product level)

1. Without consulting any external material, a Tinkerer can answer "does my device support X?" for every item in the catalog.
2. Zero reports of "stuck loading" or "results gone after rotation" in the current version.
3. After one successful sync, the app remains fully usable in airplane mode.
4. A new contributor (human or AI) can find all the logic of a given detection feature in one place within minutes, looking only at the repository's directory tree.

## 4. Open questions and pending items

| # | Question | Blocking? |
|---|---|---|
| Q1 | ~~Detection item catalog (Appendix A)~~ **resolved (2026-09-22)** — nothing is wrong with the detection items themselves; the prefilled ordered candidate list **is finalized as-is** and becomes the official item list; the Compose revamp **will not touch detection-item logic**. | Resolved |
| Q2 | **FR-7 comparison semantics** — what the server reference data contains per device / ROM, and how to present a mismatch. Decided 2026-09-12: document how each line compares in the code today as-is, add tests in batch 3 so it cannot break later, redesign when the registry structure lands in batch 5b. | Blocks FR-7 refinement, not the overall release. |
| Q3 | ~~Product name~~ **resolved (2026-09-22)** — **no rename**: the Chinese name "底层探测器" and the English / code identifier **LowLevelDetector** stay as they are; the GitHub project name is **AndroidLowLevelDetector**. It may become multi-platform via KMP/CMP later, but even on iOS it would only **view information exported from Android** (ties in with RM-1/RM-2). | Resolved |
| Q4 | **Export format** — JSON or TOML. | Blocks only RM-1. |
| Q5 | ~~Network switch~~ **resolved (2026-09-05)** — a switch in the settings "Features" group, **off by default**, currently controlling only the server json download (FR-8/FR-17). | Resolved |
| Q6 | ~~Distribution channels~~ **resolved (2026-09-05)** — `firebase` → Google Play + GitHub Releases; `foss` → GitHub Releases (a user asked for `foss` on F-Droid; shelved for now). | Resolved |
| Q7 | ~~UI language / i18n scope~~ **resolved (2026-09-22)** — the goal is to **support as many languages as possible**. Currently shipped: **Simplified Chinese, Traditional Chinese, English, French**; more will come, and **community translation contributions are welcome**. | Resolved |
| Q8 | **Custom queries and external exposure (RM-3/4/5)** — which base capabilities users may combine and open to outside access (e.g. callers of AppFunctions), and how consent / the security model is defined when systems or AI access them. | Blocks only RM-3/4/5; no effect on the current version. |
| Q9 | ~~Verdict indicator naming~~ **resolved (2026-09-22)** — **one indicator**: the dot is the visual rendering of FR-3's tri-states, scoped **to Home only** (Others/Prop are pure presentation). Color semantics grade per detection item: green = best (fully supported / already latest / good state), amber = middle (partially supported / newer / average state), red = worst (unsupported / stale / poor state; **unknown / undetectable usually also goes red**). Which color a case gets is **decided by the detection item's own semantics — no uniform cross-item standard is imposed** (no symbol fetishism); the existing judgment behavior in code will be checked item by item later, which does not block the current wording. | Resolved (see NFR-7, FR-3, FR-15) |
| Q10 | ~~Sort switch scope~~ **resolved (2026-09-12)** — verified against the code: the "sort by package name first" switch only reorders the Home screen's "outdated targetSdk apps" list (the sort branch of `HomeRepository.getOutdatedTargetSdkVersionApkModel()`). | Resolved |

## Appendix A — Detection item catalog

Maintained as a separate document: "[Detection item catalog](05-detection-item-catalog.md)" (status: **finalized 2026-09-22, Q1 resolved** — the ordered candidate list becomes the official list as-is).

- FR-1 / FR-2 reference it at the **category level**; the item-level list exists only in the catalog file; future additions / adjustments change only that file.
- Items correspond **one-to-one** with the detection engine's registry (see the detection engine section of "[04 — Architecture and decisions](04-architecture-and-decisions.md)").
