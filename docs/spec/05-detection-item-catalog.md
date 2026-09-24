# 05 — Detection item catalog (Appendix A)

Part of the "[LowLevelDetector specification](README.md)".

| | |
|---|---|
| **Document** | The official item-level detection list |
| **Status** | **Finalized (2026-09-22; Q1 resolved).** The official ordered item list, transcribed from the call order of the owner-reviewed `HomeViewModel.detect()` / `OthersViewModel.collectModels()` / `PropViewModel.collectModels()` (2026-09-05). The detection items themselves are sound and the logic is stable; **the Compose revamp will not touch detection-item logic**. |
| **Date** | 2026-09-22 |
| **Counting rule** | Counted by **detector method**: Home 23 + Others 37 + Prop 5 = **65 items**. That is the unit used for review and scheduling and **does not equal the number of screen rows**. `HomeRepository.detectSecurityPatches()` and `HomeRepository.detectTrebleAndGsiCompatibility()` each produce 2 rows (so the Home screen shows 25 rows); Others' partition fingerprints and Prop's per-property tables produce multiple dynamic rows depending on device state. |

## Scope note

Tri-state verdicts (FR-3) and verdict dots (FR-15) apply to **Home** items only; **Others and Prop are pure presentation** — information rows with no graded verdicts.

## Ordered items (official list)

### Home (23 items)

Transcribed from the reviewed `HomeViewModel.detect()`; no reordering without the owner's review. Item 1 is the online / offline mode row at the top.

| # | Item | Source call / notes |
|---|---|---|
| 1 | Detection mode (online / offline) | `detectMode`; reflects the settings switch and data source (FR-16) |
| 2 | Android version | `detectAndroid` |
| 3 | SDK extension version | `detectSdkExtension` |
| 4 | Build ID | `detectBuildId` |
| 5 | Security patches | `detectSecurityPatches` |
| 6 | Performance class | `detectPerformanceClass` |
| 7 | Kernel | `detectKernel` |
| 8 | A/B (seamless updates) | `detectAb` |
| 9 | system-as-root (SAR) | `detectSar` |
| 10 | Dynamic partitions | `detectDynamicPartitions` |
| 11 | Treble and GSI compatibility | `detectTrebleAndGsiCompatibility` |
| 12 | DSU (Dynamic System Updates) | `detectDsu` |
| 13 | Mainline (Google Play system updates) | `detectMainline` |
| 14 | VNDK (vendor NDK) | `detectVndk` |
| 15 | APEX (flattened) | `detectApex` |
| 16 | Developer options | `detectDeveloperOptions` |
| 17 | ADB | `detectAdb` |
| 18 | ADB authentication | `detectAdbAuthentication` |
| 19 | Encryption | `detectEncryption` |
| 20 | SELinux | `detectSELinux` |
| 21 | Toybox | `detectToybox` |
| 22 | WebView implementation | `detectWebView` |
| 23 | Outdated targetSdk apps | `getOutdatedTargetSdkVersionApkModel`; details can be refreshed on their own (belongs to the single-row refresh FR-13 mentions; scope note in FR-14) |

### Others (37 items)

Transcribed from the reviewed `OthersViewModel.collectModels()`; items limited by API level are marked in the notes.

| # | Item | Source call / notes |
|---|---|---|
| 1 | Brand | `getBrand` |
| 2 | Manufacturer | `getManufacturer` |
| 3 | Model | `getModel` |
| 4 | Device | `getDevice` |
| 5 | Product | `getProduct` |
| 6 | Hardware | `getHardware` |
| 7 | Board | `getBoard` |
| 8 | SoC model | `getSocModel`; Android 12+ |
| 9 | SoC manufacturer | `getSocManufacturer`; Android 12+ |
| 10 | SKU | `getSku`; Android 12+ |
| 11 | Vendor SKU | `getVendorSku`; Android 12+ |
| 12 | ODM SKU | `getOdmSku`; Android 12+ |
| 13 | Binder state (binder driver) | `getBinderStatus` |
| 14 | Process bitness | `getProcessBit` |
| 15 | Process / VM architecture | `getArchitecture` |
| 16 | CPU ABI (Build field) | `getCpuAbi` |
| 17 | CPU ABI (system property) | `getPropertyCpuAbi` |
| 18 | Supported 32-bit ABIs | `getSupported32BitAbis` |
| 19 | Supported 64-bit ABIs | `getSupported64BitAbis` |
| 20 | Build user | `getUser` |
| 21 | Build host | `getHost` |
| 22 | Build time | `getTime` |
| 23 | Base OS | `getBaseOs` |
| 24 | Fingerprint | `getFingerprint` |
| 25 | Preview SDK fingerprint | `getPreviewSdkFingerprint`; Android 10+ |
| 26 | Partition fingerprints | `getPartitionFingerprints` |
| 27 | Build Id | `getId` |
| 28 | Display | `getDisplay` |
| 29 | Build type | `getType` |
| 30 | Build tags | `getTags` |
| 31 | Incremental | `getIncremental` |
| 32 | Version codename | `getCodename` |
| 33 | Preview SDK int | `getPreviewSdkInt` |
| 34 | Default user agent | `getDefaultUserAgent` |
| 35 | Kernel version | `getKernelVersion` |
| 36 | Bootloader | `getBootloader` |
| 37 | Baseband version | `getRadioVersionOrNull` |

### Prop (5 items)

Transcribed from the reviewed `PropViewModel.collectModels()`.

| # | Item | Source call / notes |
|---|---|---|
| 1 | System properties (`getprop`) | `getSystemProp` |
| 2 | Settings.System table | `getSettings(Settings.System)` |
| 3 | Settings.Secure table | `getSettings(Settings.Secure)` |
| 4 | Settings.Global table | `getSettings(Settings.Global)` |
| 5 | build.prop | `getBuildProp` |

## Item template (when adding / adjusting items)

| Field | Meaning |
|---|---|
| **id** | Stable identifier used by the detection engine registry |
| **screen** | Home / Others / Prop |
| **title** | User-visible name |
| **question** | What question this item answers, in plain words |
| **techniques** | Allowed detection tiers (FR-9: public API / reflection / shell / root-Shizuku) |
| **tri-state semantics** | What counts as supported / unsupported / unknown, and the corresponding dot color — specified by the item's own semantics (see FR-3, FR-15, NFR-7; Q9 resolved) |

Items correspond **one-to-one** with the detection engine registry — see the detection engine section of "[04 — Architecture and decisions](04-architecture-and-decisions.md)".
