# 01 — Product and users

Part of the "[LowLevelDetector specification](README.md)".

## 1. Introduction

### 1.1 Purpose of this document

This document states what the product **must do** (functional requirements) and **how far it must go** (non-functional requirements), draws the scope, sets priorities, and spells out what "done" means for each item. Whether a feature belongs to the current version, the backlog, or the roadmap is settled by this document.

How it is implemented is deliberately not elaborated here; that belongs to the architecture part of the specification ("[04 — Architecture and decisions](04-architecture-and-decisions.md)").

### 1.2 Product scope

LowLevelDetector is a **standalone Android app**: it inspects the device it is installed on, in real time, entirely on the phone, and reports **whether the current system supports** several key Android system features.

- Its original purpose was to help the **flashing / MOD community** answer one question before flashing a Generic System Image (GSI) or a third-party ROM: "does this phone support Project Treble?"
- **Hardware specs are not the point.** Auxiliary content added later covers **system and ROM identity and state**: build / ROM identity, Android version and security patch levels, architecture and ABI (including Binder bitness), kernel version, WebView user agent, and full dumps of raw system properties and `Settings.*` (the Prop screen). **The app does not actively read memory, screen, or battery** — such values can only appear passively, as opaque content the system returns, inside the Prop screen dumps; the app neither parses nor judges them. The property families are listed under FR-2 in "[02](02-functional-requirements.md)", item by item in "[05](05-detection-item-catalog.md)".
- The current version has **no data export of any form** — results are looked up on the phone on the spot and only viewed in the app (NG1-NG2; possible future work is in the release scope list in "[03](03-nfr-release-and-open-questions.md)").

### 1.3 Terms and abbreviations

| Term | Meaning |
|---|---|
| **Treble** | Project Treble: Android's scheme for splitting the "vendor part" and the "Google part" of the system (vendor partition + stable vendor HAL interfaces). This app was originally built to check it. |
| **VINTF** | The compatibility manifest / framework Treble uses to check whether vendor interfaces match. |
| **A/B (seamless) updates** | Dual-slot partition scheme: updates download and install in the background; one reboot switches to the new system. |
| **Dynamic partitions / SAR** | The newer partition scheme (the super partition; system-as-root). |
| **GSI** | Generic System Image. |
| **Tri-state result** | The conclusion model for each item: **supported / unsupported / unknown**. |
| **SSOT** | Single Source of Truth — one piece of state has exactly one authoritative place. |
| **UDF** | Unidirectional Data Flow — operations travel from the UI toward the data layer and data flows back to the UI; neither side writes the other's state. |
| **KMP / CMP** | Kotlin Multiplatform / Compose Multiplatform — the intended technology for future cross-platform work. |
| **Shizuku** | A mechanism that gives an app ADB-level privileges without full root. |
| **minSdk** | The lowest supported Android API level. |
| **FOSS** | Free and Open-Source Software; in this product it means a release **without proprietary SDKs and without data collection**. |
| **Variants (`firebase` / `foss`)** | The product's two release builds — see FR-10 ("[02 — Functional requirements](02-functional-requirements.md)"). |

### 1.4 Stakeholders and intended readers

- **The product owner / maintainer** (the primary reader of this document).
- **Future contributors, human or AI-assisted**: the requirements are written clearly enough that nobody has to guess.

## 2. Users and personas

### 2.1 Primary persona — the flashing hobbyist ("Tinkerer")

- **Persona**: technically skilled; flashes GSIs / third-party ROMs; understands words like "partition" and "slot".
- **Needs**: before flashing, know whether the device supports the system features needed; after flashing, verify what the new system actually provides.
- **Success**: open the app, understand the tri-state answers, and go flashing with confidence.

### 2.2 Secondary persona — the curious ordinary user ("Explorer")

- **Persona**: anyone who wants to understand their phone's system; possibly no jargon knowledge at all.
- **Needs**: a trustworthy "what does my phone support" list that does not leave them baffled; the three states explained in plain words.
- **Success**: understands the list without looking anything up elsewhere; unknown items look exactly like "unknown" and never mislead.

> By the owner's decision, the product targets **"anyone who wants to understand their phone's system"**; the Tinkerer is only the starting point and the most demanding of those users.

## 3. Product overview

### 3.1 Background

Every time Google ships a new Android platform capability (Treble was the first example), the flashing community immediately wants to know which devices can use it. The answers are scattered across vendor pages, forum posts, and the process of digging through build.prop. This app gathers the answers into a list you can check yourself inside the app: open it, read the list, know what your system supports.

### 3.2 Positioning statement

> For anyone who wants to understand their phone's system — especially flashing hobbyists — LowLevelDetector is an on-phone inspection tool that lists, item by item, how the current system supports key Android features, each with a simple tri-state conclusion.

### 3.3 Value proposition

1. **Answers readable at a glance** — the conclusions up top, without having to chew through raw dump data yourself.
2. **It checks the phone itself** — it reflects the *currently installed* system, not a marketing spec sheet.
3. **Works offline** — online reference data is cached locally; the app works without any network.
4. **One broken item never breaks the whole** — a failing detection item can never spoil the entire report.

### 3.4 Goals (current version)

- G1: reliable tri-state detection results across a curated set of Android system features.
- G2: results survive rotation and UI recreation, with no repeated loading and no hangs.
- G3: offline-first by design: online reference data is downloaded and cached in the app's own directory.
- G4: code organized by feature so both humans and AI assistants can find what to change (organized along SSOT/UDF lines).

### 3.5 Non-goals (current version)

- NG1: **no data export** — no JSON/TOML output, no sharing, no copying the whole report (the system's long-press text selection is the only way to copy).
- NG2: **no cross-platform viewer** — the Compose Multiplatform viewer for exported data exists only on the roadmap.
- NG3: **no per-item detail page** — expanding an item to see details (raw values, the basis for the verdict, a plain-language explanation) is future work, not in this version.
- NG4: **no benchmarks or deep hardware info** — hardware info is limited to the auxiliary "system parameters" section.
- NG5: **never modifies the system** — strictly read-only: inspect, never touch.
- NG6: **no anti-detection / hidden features** — the app reports the environment truthfully and does not help disguise it.

### 3.6 Roadmap highlights

The roadmap and backlog live in **a single list** — the "release scope and roadmap list" in "[03 — Non-functional requirements, release, and open questions](03-nfr-release-and-open-questions.md)" (maintained only there; nothing repeats it elsewhere, so a change in one place cannot be forgotten in another). Themes at a glance: export in a generic data format (RM-1), a cross-platform viewer (RM-2; possibly evolving toward KMP/CMP — even on iOS it would only *view* information exported from Android), richer interaction (BL-1/BL-3; BL-2 copy / share is also confirmed, timing and interaction to be decided before work starts), open queries and AI (RM-3/4/5, pending Q8). The product name is settled (Q3, 2026-09-22): keep the names as they are — Chinese "底层探测器", English / code identifier **LowLevelDetector** — **no rename**.

### 3.7 Variants and data collection (build variants)

The product ships in two release builds with **identical detection features**:

| Variant | Data collection | Distribution |
|---|---|---|
| **`foss`** | **None** — no proprietary SDKs, no telemetry. | GitHub Releases. |
| **`firebase`** | Firebase **Crashlytics** (crash reports) and **Analytics** (usage data) collect user information — **on by default, with no runtime switch to turn it off today** (a dedicated telemetry switch may come later, BL-7). | Google Play and GitHub Releases. |

The in-app network switch (FR-8) governs **reference data sync only**; it does not control Firebase telemetry. The privacy policy opens from the app's settings screen (in a browser; the policy file lives in this repository). Full requirements in FR-10.
