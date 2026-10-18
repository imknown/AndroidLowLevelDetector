# Product and users

> **Status**: **living**, part of [the requirements](README.md).

## 1. Purpose and scope

### 1.1 Purpose of this chapter

This chapter states what the product **must do** (functional requirements) and **how far it must go**  
(non-functional requirements): it draws the scope, sets priorities, and spells out what "done" means for each  
item, including whether a capability belongs to the current version, the backlog, or the roadmap.  
How any of it is implemented is not described here; the architecture as built lives in  
[the architecture reference](../agents/architecture.md).

### 1.2 Product scope

LowLevelDetector is a **standalone Android app**: it inspects the device it is installed on, in real time,  
entirely on the phone, and reports **whether the current system supports** several key Android system features.

- Its original purpose was to help the **flashing / MOD community** answer one question before flashing a  
  Generic System Image (GSI) or a third-party ROM: "does this phone support Project Treble?"
- **Hardware specs are not the point.** The auxiliary content covers **system and ROM identity and state**:  
  build / ROM identity, Android version and security patch levels, architecture and ABI (including Binder  
  bitness), kernel version, WebView user agent, and full dumps of raw system properties and `Settings.*`  
  tables (the Prop screen). The app does not actively read memory, screen, or battery: values of that kind can  
  only appear passively, as opaque content the system returns, inside the Prop screen dumps, and the app  
  neither parses nor judges them. The property families are stated in [FR-2](02-functional-requirements.md)  
  and item by item in [the detection item catalog](04-detection-item-catalog.md).
- The current version has **no data export of any form**: results are looked up on the phone on the spot and  
  only viewed in the app (NG1, NG2); possible future work is listed in  
  [the release and roadmap chapter](03-non-functional-requirements-and-roadmap.md).

### 1.3 Terms and abbreviations

| Term | Meaning |
|---|---|
| **Treble** | Project Treble: Android's scheme for splitting the "vendor part" and the "Google part" of the system (vendor partition + stable vendor HAL interfaces). This app was originally built to check it. |
| **VINTF** | The compatibility manifest / framework Treble uses to check whether vendor interfaces match. |
| **A/B updates** | Dual-slot partition scheme: updates download and install in the background; one reboot switches to the new system. |
| **Dynamic partitions / SAR** | The newer partition scheme (the super partition; system-as-root). |
| **GSI** | Generic System Image. |
| **Tri-state result** | The conclusion of a Home detection item: **supported / unsupported / unknown**. |
| **SSOT** | Single Source of Truth: one piece of state has exactly one authoritative place. |
| **UDF** | Unidirectional Data Flow: operations travel from the UI toward the data layer, and data flows back to the UI; neither side writes the other's state. |
| **KMP / CMP** | Kotlin Multiplatform / Compose Multiplatform: the intended technology for the cross-platform viewer on the roadmap. |
| **Shizuku** | A mechanism that gives an app ADB-level privileges without full root. |
| **minSdk** | The lowest supported Android API level. |
| **FOSS** | Free and Open-Source Software; here it means a release **without proprietary SDKs and without data collection**. |
| **Variants (`firebase` / `foss`)** | The product's two release builds, see [3.7](#37-variants-and-data-collection). |

### 1.4 Stakeholders and intended readers

- **The maintainer**, the primary reader of this document.
- **Future contributors, human or AI-assisted**: the requirements are written clearly enough that nobody has  
  to guess.

## 2. Users and personas

### 2.1 Primary persona: the flashing hobbyist ("Tinkerer")

- **Persona**: technically skilled; flashes GSIs and third-party ROMs; understands words like "partition" and  
  "slot".
- **Needs**: before flashing, know whether the device supports the system features needed; after flashing,  
  verify what the new system actually provides.
- **Success**: opens the app, understands the tri-state answers, and goes flashing with confidence.

### 2.2 Secondary persona: the curious ordinary user ("Explorer")

- **Persona**: anyone who wants to understand their phone's system; possibly no jargon knowledge at all.
- **Needs**: a "what does my phone support" list that is trustworthy and explainable; the three states  
  explained in plain words.
- **Success**: understands the list without looking anything up elsewhere; unknown items look exactly like  
  "unknown" and never mislead.

The product targets **anyone who wants to understand their phone's system**; the Tinkerer is the starting point  
and the most demanding of those users.

## 3. Product overview

### 3.1 Background

Every time Google ships a new Android platform capability (Treble was the first example), the flashing  
community immediately wants to know which devices can use it. The answers are scattered across vendor pages,  
forum posts, and the work of digging through build.prop. This app gathers the answers into one list to check  
inside the app: open it, read the list, know what the system supports.

### 3.2 Positioning statement

For anyone who wants to understand their phone's system, especially flashing hobbyists, LowLevelDetector is an  
on-phone inspection tool that lists, item by item, how the current system supports key Android features, each  
with a tri-state conclusion.

### 3.3 Value proposition

1. **Conclusions readable at a glance**: the verdict sits on the row itself, instead of having to be derived  
   from raw dump data.
2. **It checks the phone itself**: it reflects the currently installed system, not a marketing spec sheet.
3. **Works offline**: the online reference data is cached in the app's own directory, so the app works with  
   no network.
4. **One broken item never breaks the whole**: a failing detection item cannot spoil the rest of the report.

### 3.4 Goals (current version)

- G1: reliable tri-state detection results across the curated set of Android system features.
- G2: results survive rotation and UI recreation, with no repeated loading and no hangs.
- G3: offline-first: the online reference data is downloaded and cached in the app's own directory.
- G4: code organized by feature, so both humans and AI assistants can find what to change (along SSOT / UDF  
  lines).

### 3.5 Non-goals (current version)

- NG1: **no data export**: no JSON / TOML output, no sharing, no copying the whole report (the system's  
  long-press text selection is the only way to copy).
- NG2: **no cross-platform viewer**: the Compose Multiplatform viewer for exported data exists only on the  
  roadmap.
- NG3: **no per-item detail page**: expanding an item to see the raw values behind a verdict is future work.
- NG4: **no benchmarks or deep hardware info**: hardware information is limited to the auxiliary system  
  parameters.
- NG5: **the app never modifies the system**: strictly read-only, it inspects and never touches.
- NG6: **no anti-detection features**: the app reports the environment truthfully and does not help disguise  
  it.

### 3.6 Roadmap highlights

The backlog and the roadmap live in a single list, the release scope and roadmap table in  
[the release and roadmap chapter](03-non-functional-requirements-and-roadmap.md); nothing repeats it  
elsewhere, so a change in one place cannot be forgotten in another. Themes at a glance: export in a generic  
data format (RM-1), a cross-platform viewer (RM-2), richer interaction (BL-1, BL-2, BL-3), and open queries  
with external exposure (RM-3, RM-4, RM-5, pending Q8). The product names stay as they are: the Chinese name  
"底层探测器", the English and code identifier **LowLevelDetector**, and the GitHub project  
**AndroidLowLevelDetector** (Q3, settled).

### 3.7 Variants and data collection

The product ships as two release builds with **identical detection features**:

| Variant | Data collection | Distribution |
|---|---|---|
| **`foss`** | **None**: no proprietary SDKs, no telemetry. | GitHub Releases. |
| **`firebase`** | Firebase **Crashlytics** (crash reports) and **Analytics** (usage data), **on by default with no runtime switch to turn them off** (a dedicated telemetry switch may come later, BL-7). | Google Play and GitHub Releases. |

The app's network switch governs **reference data sync only** (FR-8); it does not control Firebase telemetry.  
The privacy policy opens from the settings screen in a browser, and the policy file lives in this repository  
(FR-10).
