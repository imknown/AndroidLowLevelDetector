# 02 — Functional requirements

Part of the "[LowLevelDetector specification](README.md)".

Priority definitions: **P0** = required in the current version · **P1** = should have · **P2** = nice to have · **BL** = backlog (confirmed to do, deferred) · **RM** = roadmap. The backlog and roadmap list (BL-x / RM-x and their status) is maintained in exactly one place: "[03 — Non-functional requirements, release, and open questions](03-nfr-release-and-open-questions.md)".

## 1. Requirements overview

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Feature support detection (core capability) | P0 |
| FR-2 | System parameter display (auxiliary capability) | P1 |
| FR-3 | Tri-state result model | P0 |
| FR-4 | Auto-detect on screen entry (merged into FR-13) | P0 |
| FR-5 | Pull-to-refresh re-runs detection | P0 |
| FR-6 | Per-item error isolation | P0 |
| FR-7 | Server reference data sync, comparison, and offline cache | P1 |
| FR-8 | User-controlled network switch | P1 |
| FR-9 | Lawful detection means with graceful fallbacks | P0 |
| FR-10 | Two release variants (`firebase` / `foss`) | P1 |
| FR-11 | Single Activity, single instance | P0 |
| FR-12 | Shell layout and scrolling behavior | P0 |
| FR-13 | List lifecycle: entry, lazy load, cache, restore (includes FR-4) | P0 |
| FR-14 | Shared detection list base class | P0 |
| FR-15 | Verdict dots (Home only) | P0 |
| FR-16 | Home data pipeline (online / offline / bundled / merged) | P1 |
| FR-17 | Settings screen item list | P1 |

## 2. Core capabilities (FR-1 ... FR-10)

### FR-1 Feature support detection (P0)

Detection completes on the phone itself: the current system is evaluated item by item against a **catalog of Android system feature detections** (feature families such as Treble/VINTF support, A/B seamless updates, dynamic partitions / system-as-root; the official item-level list is "[Detection item catalog](05-detection-item-catalog.md)", finalized 2026-09-22, Q1).

**Acceptance criteria**

- AC1: every item in the catalog yields exactly one tri-state result per detection run (see FR-3).
- AC2: the catalog must be sufficiently "data-driven": adding or removing an item must not require redesigning the surrounding UI (see the detection engine in the architecture chapter).
- AC3: all detection runs on the phone itself; detection never sends any user data anywhere.

### FR-2 System parameter display (P1)

The app shows common **system and ROM parameters** as auxiliary information so users can get to know their phone at a glance. Parameter families (item-level list in "[Detection item catalog](05-detection-item-catalog.md)", finalized):

- **Build / ROM identity**: brand, manufacturer, model, device, product, hardware, board; SoC model and manufacturer; various SKUs; Build ID / Display / type / tags / Incremental / codename; various partition fingerprints; bootloader and baseband versions.
- **Android version family**: version, SDK extension version, platform and vendor security patch levels, performance class (Media Performance Class).
- **Architecture and ABI**: CPU ABI, supported 32/64-bit ABIs, process bitness and VM architecture, Binder protocol bitness.
- **Kernel**, **WebView user agent**.
- **Raw property and settings dumps** (the Prop screen: `getprop` / build.prop / `Settings.System|Secure|Global`) — shown line by line as-is, not parsed, not judged.
- **Not in the parameter families**: hardware parameters such as memory, screen, and battery. Neither Home nor Others calls interfaces like `ActivityManager.MemoryInfo` / `WindowManager` / `DisplayMetrics` / `BatteryManager`, nor reads `/proc/meminfo`; such values can only appear **passively** in the Prop screen dumps as part of opaque system property / `Settings` returns (e.g. `ro.sf.lcd_density` exposed on some models) — the app neither parses nor judges them. The only near-touch is Home reading the boolean `ActivityManager.isLowRamDevice()` to add a " (Go)" suffix to the Android version row — it does not read the amount of memory. If such hardware parameters are ever displayed, they must go through the item template of "[05](05-detection-item-catalog.md)" as new items.

**Acceptance criteria**

- AC1: parameter values are read live from the device at detection time.
- AC2: parameters that cannot be read display as unknown per FR-6, never crashing or showing a blank screen.

### FR-3 Tri-state result model (P0)

The conclusion of every **Home** detection item can only be one of three states (Others/Prop are pure presentation with no verdict state — see FR-14/FR-15):

| State | Meaning |
|---|---|
| **Supported** | Detection affirmatively confirms the feature is supported / enabled. |
| **Unsupported** | Detection affirmatively confirms the feature is unsupported / disabled. |
| **Unknown** | Detection cannot give an answer — including *the detection itself failing* (throwing, API blocked, value unreadable). The tri-state is a data-level conclusion; its mapping to dot colors is in FR-15 — **unknown usually maps to red**, with the exact assignment decided by each detection item's semantics (Q9, 2026-09-22). |

**Acceptance criteria**

- AC1: the tri-state indicator is **the most important visual element** in the result list.
- AC2: there is no fourth user-visible state; error details behind an unknown item go to the log (for troubleshooting / a future detail page) and need not be displayed in this version.
- AC3: after rotation / UI recreation the same state is displayed, without re-running detection.

> For the visual rendering see FR-15; the tri-state and the dot are **one indicator**, with colors graded per detection item — ruling in "[03](03-nfr-release-and-open-questions.md)", Q9.

### FR-4 Auto-detect on screen entry (P0)

**Merged into FR-13** — the list lifecycle is specified in that one entry only: run on entry, dedupe, recreation-safe, lazy loading, caching and restore. The FR-4 ID is kept and means: *entering a screen automatically triggers one detection run, exactly once, no repeats*.

### FR-5 Pull-to-refresh (P0)

The user should be able to **pull to refresh** on a result screen, manually triggering one complete re-detection.

**Acceptance criteria**

- AC1: the refresh indicator stays visible from refresh start to end, then disappears.
- AC2: during a refresh, extra pull gestures must not start duplicate detection runs (consistent with FR-13's dedup rule).
- AC3: refresh re-runs every item; per-item isolation (FR-6) stays in effect during the refresh.

### FR-6 Per-item error isolation (P0)

When any detection item fails, **only that item is affected**.

**Acceptance criteria**

- AC1: the failed item displays as unknown; the remaining items are unaffected and stay correct.
- AC2: **there is no page-level error state**: one (or a few) failures must not replace the whole list with a screen of error, nor is an "all or nothing" retry needed.
- AC3: failures leave evidence (logs) for troubleshooting, but must never block the user.

How isolation is realized architecturally is in the architecture chapter's "error handling strategy"; how error text is shown inline in a row is in FR-14.

### FR-7 Server reference data sync, comparison, and offline caching (P1)

The app must be able to sync **server reference data** and **compare** it against what it found locally (used to cross-check / supplement displayed conclusions). Server data must be **cached in the app's own private directory**, so the app remains fully usable offline or with networking disabled.

The exact behavior of fetch / bundled fallback / version merge is specified in FR-16.

**Acceptance criteria**

- AC1: reference data lives in the app's **private directory** — the app-private external files directory (the Downloads subdirectory), with a permitted fallback to the internal files dir per Android storage norms; never in public storage — and is available offline.
- AC2: a failed sync degrades gracefully: keep using the stale cache; per FR-6 there is no page-wide error.

Fetch / parsing / version merge / offline acceptance criteria are all written under **FR-16** — maintained in that one place, not repeated.

> What exactly gets compared, and how a mismatch is presented — handling in "[03](03-nfr-release-and-open-questions.md)" Q2 (decided 2026-09-12).

### FR-8 User-controlled network switch (P1)

The app must provide a **user-visible switch** to turn **server reference data sync** (FR-7) on / off.

The switch **deliberately governs reference data sync only**, not telemetry — telemetry behavior is decided by the release variant (FR-10).

**Acceptance criteria**

- AC1: with the switch off, no reference data sync request is sent; the app reads the cache.
- AC2: the switch does not affect Firebase Crashlytics/Analytics in the `firebase` variant (telemetry is FR-10's business).
- AC3: the switch sits in the settings "Features" group, **off by default**, and currently controls only the server json download (FR-17; decided 2026-09-05).

### FR-9 Lawful detection means (P0)

Detection may use **any lawful technical means**, including but not limited to:

1. public SDK APIs;
2. reflective calls to hidden / `@hide` framework APIs and internal services;
3. shell command output (e.g. `getprop`, `dumpsys`);
4. privileged detection when root or Shizuku is available (an optional enhancement path).

**Acceptance criteria**

- AC1: the privileged path (root/Shizuku) is an **optional enhancement**; without it, fall back to ordinary detection or show unknown — never crash, and never falsely report "unsupported".
- AC2: every means used must be lawful and read-only (NG5, NG6).

### FR-10 Two release variants — `firebase` / `foss` (P1)

The product ships as two build variants with **identical detection features**. The facts of each variant (what data it collects, where it distributes, whether telemetry defaults on, F-Droid history) are set by the variants and data collection table in "[01 — Product and users](01-product-and-users.md)" — written in that one place, not repeated. A runtime telemetry switch may come later (BL-7).

**Acceptance criteria**

- AC1: the `foss` variant contains no Firebase (or other proprietary telemetry) component, directly or indirectly.
- AC2: detection features (FR-1 ... FR-6) are identical in both variants.
- AC3: in the `firebase` variant Crashlytics/Analytics are on by default and unaffected by the FR-8 network switch.
- AC4: the settings screen has a privacy policy entry that opens the online policy in a browser.

## 3. App shell and UI (FR-11 ... FR-17)

### FR-11 Single Activity, single instance (P0)

The app has exactly one **`MainActivity`**. Any app may launch it at any time with any intent flags.

**Acceptance criteria**

- AC1: the normal launch paths (launcher, recents, external apps) all reuse the existing `MainActivity` — `launchMode="singleTop"`, with repeated intents delivered via `onNewIntent`. Known issue: special flag combinations (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK`, `... | FLAG_ACTIVITY_CLEAR_TASK`) can still create a new task / instance and wipe the in-memory state; this hazard is accepted for now — task-stack protection is recorded as **BL-8** (deferred until user feedback arrives).
- AC2: after being relaunched from outside, the existing instance's state is fully preserved (not reset, no duplicate back stack).

### FR-12 Shell layout and scrolling behavior (P0)

> This FR only describes the target shape; the parts not yet delivered (scrolling behavior, large-screen NavigationRail) are recorded in 03's release list, with status not repeated here.

- Theme and widgets: **Material 3** (including Expressive color extraction).
- Root layout: a three-part Scaffold — top bar / bottom bar / content.
- Top: the **top app bar** shows the app name; the debug build's app name is **"LLD Debug"** — the launcher icon and in-app top bar share the same source.
- Bottom: the **navigation bar** with four tabs in order — **Home, Others, Prop, Settings** — each a feature screen. On large screens it shows as a **NavigationRail** (or an equivalent adaptive form).
- Middle: the screen container holding the current screen.
- All four screens can exceed one viewport and must scroll.
- Edge-to-Edge: because the bottom bar can hide, list bottom padding = **navigation bar height + bottom bar height**.

**Scrolling behavior** (deferred to the Compose batch; prefer framework defaults; keep the implementation simple; details negotiable):

- Dragging a finger up (defined as the *list scrolling down*): the top bar is slowly pushed off screen and the **bottom bar hides** (a faster animation).
- Scrolling the other way: the top bar returns and the **bottom bar reappears**; the animation must be interruptible and play backward from where it was interrupted.

**Acceptance criteria**

- AC1: scrolling down hides both the top and bottom bars; scrolling up brings both back; an interrupted animation reverses smoothly.
- AC2: bottom padding is correct in both bottom-bar states (content is never clipped, no dead zones that cannot be tapped). (Deferred with the scrolling behavior to the Compose batch; with only one bottom-bar state, the correct-padding requirement applies as usual.)

### FR-13 List lifecycle: entry, lazy load, cache, restore (P0; includes FR-4)

Applies to all four fragments — **the list lifecycle is specified in this entry only** (FR-4's entry / dedup / recreation content lives here):

- Entering a result screen automatically triggers **exactly one** detection run — first entry and every later entry alike (includes FR-4).
- Entering while a detection is already running never starts a second concurrent full run (**dedup**; FR-5's refresh is bound by the same rule).
- Detection state is held by objects that **outlive recreation**: a running detection keeps running and hands its results to the recreated screen; or an already-completed state is simply re-displayed — never reset, never re-run, never stuck on a loading icon.
- A screen the user has never opened is **never initialized and never loads data**.
- A normal first launch shows the **Home screen** (bottom nav selects Home) and loads its data.
- Tapping a tab: **show** the target screen, **then load** its data.
- Loaded screens stay **cached — data included**; the cache keeps being reused until the user refreshes (FR-5).
- After the process is killed in the background and the app is recreated: the **last displayed screen** is restored as the current tab, with data following the normal show-then-load rules (the in-memory cache died with the process; surviving ViewModel caches still apply).

**Acceptance criteria**

- AC1: cold start initializes only Home; other screens initialize on first access; each entry triggers exactly one detection — a running one is never re-run.
- AC2: switching tabs never re-runs an already loaded screen's detection, unless the user refreshes.
- AC3: after the process is killed, the last displayed screen is restored as the current tab and data loads by the normal rules.
- AC4: rotating / changing the theme during detection never resets, duplicates, or hangs the loading state.

### FR-14 Shared detection list base class (P0)

The four feature screens share one list implementation (architecture principle P4: kept together by feature):

- Built on a **reusable list**; **each row = one independent detection item**.
- Each row is a **card** made of a **title and content** (both may span multiple lines).
- **Screen roles**: **Home is the verdict screen** — every row carries a tri-state conclusion (FR-3) and a verdict dot (FR-15). **Others and Prop are pure presentation**: title + content information rows — no dots, no green/amber/red grading.
- Row content displays the detected system information; **a row contains its own exception** (FR-6), with the error message **printed straight into that row's content**.
- First display → initialize → load data (FR-13); the cache is reused until the user refreshes (FR-5).
- The official definition of each screen's item order is in "[Detection item catalog](05-detection-item-catalog.md)" (transcribed from the verified `detect()` / `collectModels()` order).
- The outdated targetSdk app check uses `QUERY_ALL_PACKAGES`, scoped to **preinstalled ROM system apps only** (never the user's own installed apps) — see the privacy policy at the repository root.

**Acceptance criteria**

- AC1: an exception in a row displays as that row's error text; other rows are unaffected (FR-6 AC1/AC2).
- AC2: the row layout supports multi-line titles and content, without truncation.

### FR-15 Verdict dots — Home only (P0)

- Every **Home** row has **a translucent dot in its top-right corner**. Others/Prop rows have **no dots and no grading** (pure presentation, FR-14).
- The dot and FR-3's tri-state are **one indicator**: the dot is the visual rendering of the tri-state conclusion (see Q9) — not a second state system, and not a crash indicator.
- Colors grade in three bands by **each detection item's own semantics**, with **no cross-item uniform mapping imposed**:
  - **green** = best: fully supported / already latest / good state;
  - **amber** = middle: partially supported / newer / average state;
  - **red** = worst: unsupported / stale / poor state; **unknown / undetectable usually also falls into red**.
- Which band a specific item gets is specified in that item's "tri-state semantics" in "[Detection item catalog](05-detection-item-catalog.md)". The verdict logic exists in code but has **not yet been verified item by item** — it will be checked against the catalog later, which does not block this entry.

**Acceptance criteria**

- AC1: every **Home** row has exactly one dot, in green / amber / red; Others/Prop rows show none.
- AC2: the state-to-color mapping is whatever the catalog specifies for that item; apart from "unknown defaults to red", no uniform mapping applies to all items.

### FR-16 Home data pipeline (online / offline / bundled / merged) (P1)

The data source is decided by the settings network switch (FR-8, **off by default**):

- **Online** (switch on): download the **server json** and cache it in the **app's private directory**. The download host is chosen by the China Standard Time timezone (Gitee vs GitHub raw); the URL is fixed to the `GIT_BRANCH` used at build time (staleness risk in the architecture chapter's risk table).
- **Download or parsing fails**: fall back to the **json bundled in the app's assets** — an ordinary file inside the APK; "using" it means **copying it into the private cache**.
- **Version merge**: if a private cache already exists, compare its version against the bundled json's version — **compared as strings in lexicographic order**; versions are ISO dates, where lexicographic order equals chronological order, and tests pin this format assumption. **Whichever is newer wins**: a newer bundled file overwrites the cache; with no cache, use the bundled one directly.
- The server one updates regularly; local debug builds can change values freely for testing.
- This entry refines FR-7 (sync + private cache + offline-first); every failure degrades per FR-6 — never a page-wide error.

**Acceptance criteria**

- AC1: with the switch off (default) or offline, detection runs entirely from the private cache or the bundled assets.
- AC2: online with a newer server json → the cache is updated and used.
- AC3: server unreachable or unparseable → fall back to the bundled one; apart from per-item isolation, the user sees no failure at all.
- AC4: the version merge always picks the newer of the cache and the bundled assets (**lexicographic comparison of ISO date version strings** — the equivalence with chronological order is locked by tests), same for the overwrite path.

### FR-17 Settings screen item list (P1)

**Tapping an entire row triggers its action** — items are not limited to switches.

**Display**

- **Theme**: follow system / force dark / force light.
- **Scrollbar mode**: off / normal / draggable.

**Features**

- **Allow network access** switch — **off by default**; currently controls **only** the server json download (implements FR-8; refines FR-16; resolves Q5).
- **Sort by package name first** switch — off by default (off = sort by targetSdk). It only reorders the Home screen's "outdated targetSdk apps" list (see Q10). **Behavior on toggle (implementation deferred until after the Compose migration)**: list empty or first load in progress → UI unchanged, data sorts by the switch when it arrives; list loaded and non-empty → immediately reorder only the "outdated targetSdk apps" row; pull-to-refresh in progress → also immediately reorder only the old row; when the refresh succeeds the new data sorts by the switch, and a failed refresh is a no-op handled by the previous two rules.

**About** (each row opens its target)

- **Store page**: pinned at **compile time** via variant resources — `foss` → the GitHub project page; `firebase` → Google Play. Zero runtime branching, no timezone branches.
- **Source code**: the GitHub project home.
- **Privacy policy**: the Google Play data safety markdown hosted in the GitHub repository (implements FR-10 AC4).
- **Open-source licenses**.
- **Current language and translator**.
- **Version** — one line per item: `versionName(versionCode)`, bundled json data version, distribution channel, installer source, first install time, last update time. Tapping this row 7 times pops a toast (easter egg, added 2026-09-06).

**Acceptance criteria**

- AC1: every listed item is present, the whole row is tappable, and behavior matches what is written above.
- AC2: theme and scrollbar options take effect immediately (or at the next recreation — if that is simpler to implement; note which one was used at implementation time).
- AC3: variant-specific items behave according to their variant; the store page obeys the variant rule above (`foss` never links to Google Play).
