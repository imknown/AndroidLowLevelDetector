# Detection item catalog

> **Status**: **living**, part of [the requirements](README.md).

The item-level list of what the app inspects, in the order each screen produces the rows. This catalog is the  
scope of [FR-1](02-functional-requirements.md) and [FR-2](02-functional-requirements.md): FR-1 covers the Home  
items, whose rows carry a verdict, and FR-2 covers everything else.

**Counting rule**: counted by detection item, not by screen row: Home 23, Others 37, Prop 5, **65 in all**.  
The Home screen shows 25 rows because two items produce two rows each. Others produces one row per partition  
for the partition fingerprints, and Prop produces one row per property, setting or build.prop line, so the row  
counts of both depend on the device.

**Verdict scope**: the tri-state (FR-3) and the verdict dot (FR-15) apply to **Home** items only.  
Others and Prop are pure presentation: information rows with no verdict and no color.

## Home (23 items)

| # | Item | Notes |
|---|---|---|
| 1 | Detection mode (online / offline) | the row at the top; it reflects the network switch and which data source was used |
| 2 | Android version | |
| 3 | SDK extension version | |
| 4 | Build ID | |
| 5 | Security patches | two rows: the platform level, then the vendor level |
| 6 | Performance class | |
| 7 | Kernel | |
| 8 | A/B updates | |
| 9 | system-as-root (SAR) | |
| 10 | Dynamic partitions | |
| 11 | Treble and GSI compatibility | two rows: Treble, then GSI |
| 12 | DSU (Dynamic System Updates) | |
| 13 | Mainline (Google Play system updates) | |
| 14 | VNDK (vendor NDK) | |
| 15 | APEX (flattened) | |
| 16 | Developer options | |
| 17 | ADB | |
| 18 | ADB authentication | |
| 19 | Encryption | |
| 20 | SELinux | |
| 21 | Toybox | |
| 22 | WebView implementation | |
| 23 | Outdated targetSdk apps | its row is the one the settings ordering switch reorders (FR-17) |

## Others (37 items)

| # | Item | Notes |
|---|---|---|
| 1 | Brand | |
| 2 | Manufacturer | |
| 3 | Model | |
| 4 | Device | |
| 5 | Product | |
| 6 | Hardware | |
| 7 | Board | |
| 8 | SoC model | Android 12+ |
| 9 | SoC manufacturer | Android 12+ |
| 10 | SKU | Android 12+ |
| 11 | Vendor SKU | Android 12+ |
| 12 | ODM SKU | Android 12+ |
| 13 | Binder state (the binder driver) | |
| 14 | Process bitness | |
| 15 | Process and VM architecture | |
| 16 | CPU ABI (the build field) | |
| 17 | CPU ABI (the system property) | |
| 18 | Supported 32-bit ABIs | |
| 19 | Supported 64-bit ABIs | |
| 20 | Build user | |
| 21 | Build host | |
| 22 | Build time | |
| 23 | Base OS | |
| 24 | Fingerprint | |
| 25 | Preview SDK fingerprint | Android 10+ |
| 26 | Partition fingerprints | one row per partition |
| 27 | Build Id | |
| 28 | Display | |
| 29 | Build type | |
| 30 | Build tags | |
| 31 | Incremental | |
| 32 | Version codename | a developer-preview suffix is appended on Android 13 and above |
| 33 | Preview SDK int | |
| 34 | Default user agent | |
| 35 | Kernel version | |
| 36 | Bootloader | |
| 37 | Baseband version | |

## Prop (5 items)

| # | Item | Notes |
|---|---|---|
| 1 | System properties (`getprop`) | one row per property, sorted by key |
| 2 | The `Settings.System` table | one row per key |
| 3 | The `Settings.Secure` table | one row per key |
| 4 | The `Settings.Global` table | one row per key |
| 5 | build.prop | one row per line of the `getprop` output |

## Item template

When an item is added or adjusted, it carries these fields:

| Field | Meaning |
|---|---|
| **id** | a stable identifier for the item |
| **screen** | Home, Others or Prop |
| **title** | the user-visible name |
| **question** | what question the item answers, in plain words |
| **techniques** | the detection means the item may use (FR-9) |
| **tri-state semantics** | what counts as supported, unsupported and unknown, and which color each gets, stated by the item's own semantics (FR-3, FR-15, NFR-7) |
