# Functional requirements

> **Status**: **living**, part of [the requirements](README.md).

Priority definitions: **P0** required in the current version, **P1** should have, **P2** nice to have,  
**BL** backlog (confirmed to do, deferred), **RM** roadmap. The backlog and roadmap list, with every  
`BL-x` / `RM-x` and its status, is maintained in exactly one place:  
[the release and roadmap chapter](03-non-functional-requirements-and-roadmap.md).

## 1. Requirements overview

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Feature support detection (core capability) | P0 |
| FR-2 | System parameter display (auxiliary capability) | P1 |
| FR-3 | Tri-state result model | P0 |
| FR-4 | Auto-detect on screen entry (stated in FR-13) | P0 |
| FR-5 | Pull-to-refresh re-runs detection | P0 |
| FR-6 | Per-item error isolation | P0 |
| FR-7 | Server reference data sync, comparison, and offline cache | P1 |
| FR-8 | User-controlled network switch | P1 |
| FR-9 | Lawful detection means with graceful fallbacks | P0 |
| FR-10 | Two release variants (`firebase` / `foss`) | P1 |
| FR-11 | Single Activity, single instance | P0 |
| FR-12 | Shell layout and scrolling behavior | P0 |
| FR-13 | List lifecycle: entry, lazy load, cache, restore (includes FR-4) | P0 |
| FR-14 | Shared detection list base | P0 |
| FR-15 | Verdict dots (Home only) | P0 |
| FR-16 | Home data pipeline (online / offline / bundled / merged) | P1 |
| FR-17 | Settings screen item list | P1 |

## 2. Core capabilities (FR-1 ... FR-10)

### FR-1 Feature support detection (P0)

Detection completes on the phone itself: the current system is evaluated item by item against the curated  
catalog of Android system features (Treble / VINTF support, A/B updates, dynamic partitions /  
system-as-root, Mainline, and so on). The item-level list is  
[the detection item catalog](04-detection-item-catalog.md).

**Acceptance criteria**

- AC1: every Home item in the catalog yields exactly one tri-state conclusion per detection run (FR-3);  
  the Others and Prop items are information rows with no conclusion (FR-14).
- AC2: adding or removing an item must not require redesigning the surrounding UI.
- AC3: all detection runs on the phone itself; detection never sends any user data anywhere.

### FR-2 System parameter display (P1)

The app shows common **system and ROM parameters** as auxiliary information, so that a user can get to know  
their phone at a glance. The parameter families, item by item in [the catalog](04-detection-item-catalog.md):

- **Build / ROM identity**: brand, manufacturer, model, device, product, hardware, board; SoC model and  
  manufacturer; the various SKUs; Build ID, Display, type, tags, Incremental, codename; partition  
  fingerprints; bootloader and baseband versions.
- **Android version family**: version, SDK extension version, platform and vendor security patch levels,  
  performance class (Media Performance Class).
- **Architecture and ABI**: CPU ABI, the supported 32-bit and 64-bit ABIs, process bitness and VM  
  architecture, Binder protocol bitness.
- **Kernel**, **WebView user agent**.
- **Raw property and settings dumps** (the Prop screen: `getprop`, build.prop, and the `Settings.System`,  
  `Settings.Secure` and `Settings.Global` tables), shown row by row as they are, not parsed and not judged.
- **Not in the parameter families**: hardware parameters such as memory, screen and battery. The app calls  
  none of `ActivityManager.MemoryInfo`, `WindowManager`, `DisplayMetrics` or `BatteryManager`, and does not  
  read `/proc/meminfo`; values of that kind can only appear **passively** in the Prop screen dumps, as part  
  of what a system property or a `Settings` table returns (for example `ro.sf.lcd_density` on some models),  
  and the app neither parses nor judges them. The one near-touch is the Android version row, which reads the  
  boolean `ActivityManager.isLowRamDevice()` to append " (Go)"; it reads no amount of memory. Should such a  
  hardware parameter ever be displayed, it enters through the item template of  
  [the catalog](04-detection-item-catalog.md) as a new item.

**Acceptance criteria**

- AC1: parameter values are read live from the device at detection time.
- AC2: a parameter that cannot be read shows as that item's failure row (FR-6), never as a crash and never as  
  a blank screen.

### FR-3 Tri-state result model (P0)

Every **Home** detection item concludes in exactly one of three states. The row renders that conclusion as its  
dot color (FR-15); the Others and Prop rows carry no conclusion at all (FR-14).

| State | Meaning |
|---|---|
| **Supported** | Detection affirmatively confirms the feature is supported, or enabled. |
| **Unsupported** | Detection affirmatively confirms the feature is unsupported, or disabled. |
| **Unknown** | Detection cannot give an answer, **the detection itself failing included** (throwing, the API being blocked, the value being unreadable). |

**Acceptance criteria**

- AC1: the tri-state indicator is the most important visual element of the Home result list.
- AC2: there is no fourth user-visible state: a failed detection shows as that item's unknown, and the error  
  text rides in that item's own row (FR-14), with the detail logged for troubleshooting.
- AC3: after rotation or another UI recreation the same state is displayed, without re-running detection.

### FR-4 Auto-detect on screen entry (P0)

**Stated in FR-13.** The ID is kept and means: entering a screen triggers one detection run, exactly once,  
with no repeats.

### FR-5 Pull-to-refresh (P0)

The user can **pull to refresh** on a result screen, which manually triggers one complete re-detection.

**Acceptance criteria**

- AC1: the refresh indicator stays visible from the start of the refresh to its end, then disappears.
- AC2: during a refresh, extra pull gestures must not start duplicate detection runs (the dedup rule of  
  FR-13).
- AC3: a refresh re-runs every item, and per-item isolation (FR-6) stays in effect throughout.

### FR-6 Per-item error isolation (P0)

When a detection item fails, **only that item is affected**.

**Acceptance criteria**

- AC1: the failed item shows as unknown, and every other item keeps its own result.
- AC2: **there is no page-level error state**: one failure, or a few, must not replace the whole list with a  
  screen of error, and no "all or nothing" retry is needed.
- AC3: a failure leaves evidence in the log for troubleshooting, but it never blocks the user.

### FR-7 Server reference data sync, comparison, and offline caching (P1)

The app syncs **server reference data** and compares it against what it found locally, to cross-check and  
supplement the displayed values. The synced data is cached in the app's own directory, so that the app stays  
fully usable offline and with networking turned off.

How the data is fetched, what it falls back to, and how its versions are merged is stated in FR-16.

**Acceptance criteria**

- AC1: the reference data lives in the app's **private** storage (the app-private external files directory,  
  or the internal files directory where the external one is unavailable), never in public storage, and it is  
  available offline.
- AC2: a failed sync degrades gracefully: the stale cache keeps being used, and per FR-6 there is no  
  page-wide error.

### FR-8 User-controlled network switch (P1)

The app provides a **user-visible switch** that turns the **server reference data sync** of FR-7 on and off.

The switch deliberately governs reference data sync only, never telemetry: whether telemetry runs is decided  
by the release variant (FR-10).

**Acceptance criteria**

- AC1: with the switch off, no sync request is sent and the app reads the cache.
- AC2: the switch does not affect Firebase Crashlytics or Analytics in the `firebase` variant.
- AC3: the switch sits in the settings screen's Function group, is **off by default**, and controls the  
  server json download only (FR-17).

### FR-9 Lawful detection means (P0)

Detection may use **any lawful technical means**, including:

1. public SDK APIs;
2. reflective calls to hidden or `@hide` framework APIs and internal services;
3. the output of shell commands such as `getprop` and `dumpsys`;
4. privileged detection when root or Shizuku is available, an optional enhancement rather than a requirement  
   of the current version.

**Acceptance criteria**

- AC1: the privileged path is an **optional enhancement**: without it detection falls back to an ordinary  
  means or reports unknown. It never crashes, and it never falsely reports "unsupported".
- AC2: every means used is lawful and read-only (NG5, NG6).

### FR-10 Two release variants (P1)

The product ships as two build variants with **identical detection features**. What each variant collects and  
where it is distributed is stated in  
[the variants table](01-product-and-users.md#37-variants-and-data-collection); a runtime telemetry switch may  
come later (BL-7).

**Acceptance criteria**

- AC1: the `foss` variant contains no Firebase, or any other proprietary telemetry, directly or indirectly.
- AC2: the detection features are identical in both variants.
- AC3: in the `firebase` variant, Crashlytics and Analytics are on by default and are not affected by the  
  FR-8 switch.
- AC4: the settings screen has a privacy policy entry that opens the online policy in a browser.

## 3. App shell and UI (FR-11 ... FR-17)

### FR-11 Single Activity, single instance (P0)

The app has exactly one `MainActivity`. Any app may launch it at any time with any intent flags.

**Acceptance criteria**

- AC1: the normal launch paths (the launcher, recents, another app) reuse the existing instance  
  (`launchMode="singleTop"`), and the app does not read a re-delivered intent. Special flag combinations  
  (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK`, and the same with `FLAG_ACTIVITY_CLEAR_TASK`) can  
  still create a new task or instance and wipe the in-memory state; that hazard is accepted for now, and  
  task-stack protection is BL-8.
- AC2: after being relaunched from outside, the existing instance's state is preserved: neither reset nor  
  duplicated into a second back stack.

### FR-12 Shell layout and scrolling behavior (P0)

> This requirement describes the target shape of the shell; which parts of it are built today is recorded in  
> [the release and roadmap chapter](03-non-functional-requirements-and-roadmap.md).

- Theme and widgets: **Material 3**, with dynamic color (Material You) where the platform provides it.
- Root layout: a three-part Scaffold of a top bar, a bottom bar, and the content.
- Top: the **top app bar** shows the app name; the debug build's app name is "LLD Debug".
- Bottom: the **navigation bar** with four tabs in this order: **Home, Others, Prop, Settings**; on large  
  screens it shows as a **navigation rail** or an equivalent adaptive form.
- Middle: the container holding the current screen.
- All four screens can exceed one viewport and must scroll.
- Edge to edge: the bottom padding under a list accounts for the navigation bar and the bottom bar, so that  
  no content is clipped and no strip is left that cannot be tapped.

**Scrolling behavior**: dragging a finger up, which scrolls the list down, pushes the top bar off screen and  
hides the bottom bar, the bottom one faster; scrolling the other way brings both back, and an interrupted  
animation reverses from where it was interrupted.

**Acceptance criteria**

- AC1: scrolling down hides both bars, scrolling up brings both back, and an interrupted animation reverses  
  smoothly.
- AC2: the bottom padding is correct in either bar state.

### FR-13 List lifecycle: entry, lazy load, cache, restore (P0, includes FR-4)

Applies to the four tabs; the list lifecycle of the three result screens, Home, Others and Prop, is  
stated in this entry only.

- Entering a result screen triggers **exactly one** detection run for that screen (FR-4); a screen that  
  already holds its data is not run again.
- Entering while a run is already in progress never starts a second concurrent run (**dedup**; the refresh of  
  FR-5 is bound by the same rule).
- Detection state is held by objects that **outlive UI recreation**: a running detection keeps running and  
  hands its result to the recreated screen, and a completed state is simply re-displayed. The state is never  
  reset, never re-run, and never left stuck on a loading icon.
- A screen the user has never opened is **never initialized and never loads data**.
- A cold start shows the **Home** screen and loads its data.
- Tapping a tab **shows** the target screen and then **loads** its data.
- A loaded screen stays **cached, its data included**; the cache keeps being reused until the user refreshes  
  (FR-5).
- After the process is killed in the background and the app is recreated, the **last displayed screen** is  
  restored as the current tab, and its data loads by the normal show-then-load rules; the in-memory cache  
  died with the process.

**Acceptance criteria**

- AC1: a cold start initializes Home only, the other screens initialize on first access, and each entry  
  triggers exactly one detection without re-running a running one.
- AC2: switching tabs never re-runs an already loaded screen's detection, unless the user refreshes.
- AC3: after the process is killed, the last displayed screen is restored as the current tab.
- AC4: rotating or changing the theme during a detection never resets, duplicates, or hangs the loading  
  state.

### FR-14 Shared detection list base (P0)

Home, Others and Prop share one list implementation, described here; the Settings screen lists preferences  
instead, in its own rows:

- Each **row is one independent detection item**.
- Each row is a **card** of a **title** and **content**, either of which may span multiple lines.
- **Screen roles**: **Home is the verdict screen**, where every row carries a tri-state conclusion (FR-3)  
  and a verdict dot (FR-15); **Others and Prop are pure presentation**, title and content rows with no dots  
  and no color grading.
- A row carries its own failure: the error text of a failed item is printed into that row's content (FR-6).
- A first display loads the row's data (FR-13), and the cache is reused until the user refreshes (FR-5).
- The item order of each screen is in [the catalog](04-detection-item-catalog.md).
- The check for outdated targetSdk apps uses `QUERY_ALL_PACKAGES`, scoped to preinstalled ROM system apps and  
  never to the user's own installed apps; the privacy policy at the repository root states this.

**Acceptance criteria**

- AC1: an exception in a row displays as that row's error text, and the other rows are unaffected (FR-6).
- AC2: the row layout supports multi-line titles and content without truncation.

### FR-15 Verdict dots (Home only) (P0)

- Every **Home** row carries **one translucent dot**, at the top end of its title; the Others and Prop rows  
  carry no dot and no grading.
- The dot and the tri-state of FR-3 are **one indicator**: the dot is the visual rendering of the conclusion,  
  not a second state system and not a crash indicator.
- The colors grade in three bands by **each detection item's own semantics**, with no uniform cross-item  
  mapping imposed: **green** for the best state (fully supported, already latest, good), **amber** for the  
  middle one (partially supported, newer, average), **red** for the worst one (unsupported, stale, poor), and  
  **unknown or undetectable usually falls into red as well**.
- Which band a given item gets is decided by that item's own semantics.

**Acceptance criteria**

- AC1: every Home row carries exactly one dot, in one of the three colors, and the Others and Prop rows show  
  none.
- AC2: the state-to-color mapping is whatever that item's own semantics say; apart from "unknown usually  
  falls into red", no uniform mapping applies to every item.

### FR-16 Home data pipeline (P1)

Which data the Home screen uses is decided by the network switch (FR-8, **off by default**):

- **Online** (the switch on): download the **server json** and cache it in the app's private directory. The  
  download host is chosen by the phone's timezone, the Chinese mainland timezones using Gitee and the rest  
  using GitHub raw, and the URL is pinned to the `GIT_BRANCH` of the build.
- **The download or its parsing fails**: keep using the **private cache**, and fall back to the **json  
  bundled in the app's assets** when no cache is usable or when the bundled one is newer (the version merge  
  below); using the bundled one means copying it into the cache.
- **Version merge**: when a private cache already exists, its version is compared against the bundled json's  
  version **as strings**. The versions are `yyyy-MM-dd HH:mm Z` datetimes, whose string order is their  
  chronological order, so the newer one wins: a newer bundled file overwrites the cache, and with no cache  
  the bundled one is used.
- Every failure degrades per FR-6, and there is never a page-wide error.

**Acceptance criteria**

- AC1: with the switch off (the default) or with no network, detection runs entirely from the private cache  
  or from the bundled assets.
- AC2: online, with a newer server json, the cache is updated and used.
- AC3: a server that is unreachable or unparseable falls back to the private cache or to the bundled json,  
  and apart from per-item isolation the user sees no failure at all.
- AC4: the version merge always picks the newer of the cache and the bundled assets by the string comparison  
  above.

### FR-17 Settings screen item list (P1)

Tapping an entire row triggers its action; not every row is a switch.

**Interface**

- **Themes**: follow system, always light, always dark.
- **Scroll bar mode**: none, normal.

**Function**

- **Allow network data** switch, **off by default**: it controls the server json download only (FR-8, FR-16).
- **Order by package name first** switch, off by default: with it off, the outdated targetSdk apps row  
  lists the apps by targetSdk, and with it on, by package name. It changes that one row of the Home screen  
  and nothing else. Toggled while the list is already loaded, that row updates immediately; toggled while  
  the first load is still running, the list appears in the new order when the data arrives.

**About**, each row opening its target:

- **Store page**, pinned at compile time by the variant: `foss` opens the GitHub project page and `firebase`  
  opens Google Play, with no runtime branching.
- **Source code**: the GitHub project home.
- **Privacy policy**: the policy markdown hosted in the GitHub repository (FR-10 AC4).
- **License**: the open-source repositories the app references.
- **Language and translator**: the current language and who translated it.
- **Version**: the version name and code, the built-in data version, the distributor, the installer source,  
  the first install time and the last update time. Tapping this row 7 times shows a toast.

**Acceptance criteria**

- AC1: every listed item is present, the whole row is tappable, and each behaves as stated above.
- AC2: the theme and scroll bar options take effect without a restart.
- AC3: the variant-specific items follow their variant, and the `foss` store page never links to Google Play.
