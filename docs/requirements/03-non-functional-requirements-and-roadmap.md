# Non-functional requirements, release, and roadmap

> **Status**: **living**, part of [the requirements](README.md).

## 1. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Compatibility | The app runs on the API level declared as `minSdk` in `gradle/toml/build.toml` and above (Android 7.0 today), following the AndroidX floor. On old or vendor-modified systems, feature detection must degrade gracefully, showing unknown, and must never crash. |
| NFR-2 | Privacy | Detection in both variants runs on the phone, and detection results are never uploaded. Whether reference data sync (FR-7) uses the network is the user's choice (FR-8, sync only). Cached reference data lives only in the app's private storage. Telemetry follows the variant (FR-10): the `firebase` variant collects crash reports and usage data through Firebase, on by default with no runtime switch today, while the `foss` variant collects nothing. |
| NFR-3 | Performance | Detection runs asynchronously; the result screen appears promptly, and detection work never blocks the main thread. |
| NFR-4 | Reliability | State survives UI recreation, and after process death the screen is restored, per the lazy-load and cache rules of FR-13; a refresh neither deadlocks nor stacks (FR-5); a failure stays isolated to one item (FR-6). |
| NFR-5 | Maintainability | The codebase must be easy for both humans and AI assistants to navigate: one source of truth for state (SSOT), one-way data flow (UDF), and code kept together by feature, so that a feature's logic sits in one place instead of being scattered across distant directories. |
| NFR-6 | Legality and security | Only lawful, read-only means (FR-9), and no evasion tricks (NG6). |
| NFR-7 | Readability | The tri-state model must be understandable to a non-expert, the Explorer persona. The verdict dot (Home only) is the visual rendering of the tri-states, one indicator: green for the best state, amber for the middle one, red for the worst one, and unknown or undetectable usually falls into red as well. Which color a case gets follows that item's own semantics, and no uniform cross-item mapping is imposed. |
| NFR-8 | Adaptivity | The UI uses adaptive layouts: phone, tablet, foldable, window resizing, and multi-window all work. This states the target; what is built today is recorded in the release scope and roadmap table below. |

## 2. Release scope and roadmap

This is the only list of its kind; nothing else repeats it.

What the marks mean: a ✅ in the Current column means the code implements that capability today;  
a ✅ in the Backlog or Roadmap column means that is where the capability is planned to go, not that it is  
done.

| ID | Capability | Current | Backlog | Roadmap |
|---|---|---|---|---|
| FR-1 | Feature support detection (tri-state results) | ✅ | | |
| FR-2 | System parameters (the auxiliary layer) | ✅ | | |
| FR-13 | Detection on entry, lazy loading, caching, restore (includes FR-4) | ✅ | | |
| FR-5 | Pull-to-refresh | ✅ | | |
| FR-6 | Per-item error isolation | ✅ | | |
| FR-7, FR-16 | Server reference data sync, offline cache, version merge | ✅ | | |
| FR-8, FR-17 | Network switch (sync only, off by default) | ✅ | | |
| FR-10 | Two variants (`foss` / `firebase`) | ✅ | | |
| FR-11 | Single Activity, single instance | ✅ | ✅ (BL-8) | |
| FR-12 | Shell layout: the three-part Scaffold, the four tabs, the insets | ✅ | | |
| FR-12 | The scrolling behavior, and the large-screen navigation rail | | ✅ | |
| FR-14, FR-15 | Detection list rows, and the Home verdict dots | ✅ | | |
| NFR-8 | Adaptive layouts: tablet, foldable, window resizing, multi-window | | ✅ | |
| BL-7 | Runtime telemetry switch (`firebase`) | | ✅ | |
| BL-1 | Per-item detail expansion, showing the raw values behind a verdict; not doing it for now, so a card keeps only its ripple feedback and has no click action | | ✅ | |
| BL-2 | Copy, and share (both; the timing and the interaction are settled before the work starts) | | ✅ | |
| BL-3 | Single-row refresh | | ✅ | |
| BL-8 | Task-stack launch protection for the special intent flag combinations (FR-11 hardening), deferred until feedback from users arrives | | ✅ | |
| RM-1 | Export in a generic data format (the format is Q4) | | | ✅ |
| RM-2 | Cross-platform viewer (Compose Multiplatform) | | | ✅ |
| RM-3 | User-defined queries (Q8) | | | ✅ |
| RM-4 | An external query surface, such as AppFunctions (Q8) | | | ✅ |
| RM-5 | Native AI interaction | | | ✅ |

## 3. Success criteria (product level)

1. Without consulting any external material, a Tinkerer can answer "does my device support X?" for every item  
   in the catalog.
2. No reports of "stuck loading" or "results gone after rotation" in the current version.
3. After one successful sync, the app remains fully usable in airplane mode.
4. A new contributor, human or AI, can find all the logic of a given detection feature in one place within  
   minutes, looking only at the repository's directory tree.

## 4. Open questions

| # | Question | Status |
|---|---|---|
| Q1 | The detection item catalog: settled. The item list is final, and the detection items themselves are sound. | Settled |
| Q2 | The comparison semantics of FR-7: what the server reference data contains per device and ROM, and how a mismatch is presented. The comparison stays as the code does it today until the shape of the reference data changes, and a redesign starts from there. | Open (the direction is settled, the semantics are not designed) |
| Q3 | The product name: settled, no rename. The Chinese name "底层探测器", the English and code identifier LowLevelDetector, and the GitHub project AndroidLowLevelDetector all stay. A multiplatform future would only view information exported from Android (RM-1, RM-2). | Settled |
| Q4 | The export format: JSON or TOML. | Open (blocks RM-1 only) |
| Q5 | The network switch: settled. A switch in the settings screen's Function group, off by default, controlling the server json download only (FR-8, FR-17). | Settled |
| Q6 | Distribution channels: settled. `firebase` goes to Google Play and GitHub Releases, `foss` goes to GitHub Releases; `foss` on F-Droid was asked for and is shelved for now. | Settled |
| Q7 | The UI language scope: settled. The goal is to support as many languages as possible; shipped today are English, Simplified Chinese, Traditional Chinese and French, and community translations are welcome. | Settled |
| Q8 | Custom queries and external exposure (RM-3, RM-4, RM-5): which base capabilities may be combined and opened to outside callers, and how consent and the security model are defined when a system or an assistant accesses them. | Open (blocks RM-3, RM-4 and RM-5 only) |
| Q9 | The verdict indicator: settled. One indicator, the dot being the visual rendering of the tri-states of FR-3, scoped to Home; the color of a case is decided by that item's own semantics, with no uniform cross-item standard. | Settled |
| Q10 | The scope of the ordering switch: settled. The "order by package name first" switch reorders the Home screen's outdated targetSdk apps row, and nothing else. | Settled |

The detection item catalog is maintained in [the detection item catalog](04-detection-item-catalog.md): FR-1  
and FR-2 reference it at the category level, and the item-level list exists only there.
