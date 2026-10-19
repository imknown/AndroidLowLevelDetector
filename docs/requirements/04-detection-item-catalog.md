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

**Item ids**: `HOME-01` ... `HOME-23`, `OTH-01` ... `OTH-37`, `PROP-01` ... `PROP-05`. The number is the  
position in the screen's list, and a screen's list order is the order in which the feature ViewModel builds  
it: `collectModels()` for Others and Prop, and the `detect()` call it makes for Home. A new item appended to  
the end of a list takes the next number; an item inserted in the middle renumbers the items after it, and  
every document that cites an id is updated in the same change.

**Failure rows**: every Home item's failure row is red and carries the error text in that row's content (FR-6).  
The definitions below therefore name only the outcomes the item itself distinguishes.

## Home (23 items)

| id | Item | Notes |
|---|---|---|
| HOME-01 | Detection mode (online / offline) | the row at the top; it reflects the network switch and which data source was used |
| HOME-02 | Android version | |
| HOME-03 | SDK extension version | |
| HOME-04 | Build ID | |
| HOME-05 | Security patches | two rows: the platform level, then the vendor level |
| HOME-06 | Performance class | |
| HOME-07 | Kernel | |
| HOME-08 | A/B updates | |
| HOME-09 | system-as-root (SAR) | |
| HOME-10 | Dynamic partitions | |
| HOME-11 | Treble and GSI compatibility | two rows: Treble, then GSI |
| HOME-12 | DSU (Dynamic System Updates) | |
| HOME-13 | Mainline (Google Play system updates) | |
| HOME-14 | VNDK (vendor NDK) | |
| HOME-15 | APEX (flattened) | |
| HOME-16 | Developer options | |
| HOME-17 | ADB | |
| HOME-18 | ADB authentication | |
| HOME-19 | Encryption | |
| HOME-20 | SELinux | |
| HOME-21 | Toybox | |
| HOME-22 | WebView implementation | |
| HOME-23 | Outdated targetSdk apps | its row is the one the settings ordering switch reorders (FR-17) |

### Home item definitions

#### HOME-01 Detection mode (online / offline)

- **Question**: which reference data did this run use, and which datetime is it from?
- **Techniques**: the app's own reference data, from the server, the private cache, or the bundled assets  
  (FR-16).
- **Tri-state semantics**: green when the server json was fetched, parsed and used; red when the run used the  
  private cache or the bundled json, including the case where an online attempt failed and the run continued  
  offline; red when no json could be read at all, where the version position shows `Unknown`. This item has no  
  amber outcome.

#### HOME-02 Android version

- **Question**: is this Android version the latest one, and does upstream still support it?
- **Techniques**: a public SDK API (`Build.VERSION`) compared against the reference data.
- **Tri-state semantics**: green when the device runs the latest stable or the latest preview release; amber  
  when it is not the latest but upstream still supports it; red when upstream no longer supports it, and also  
  red when there is no reference data for the comparison.

#### HOME-03 SDK extension version

- **Question**: does the device support the SDK extension level of the current stable Android release?
- **Techniques**: a public SDK API (`Build.VERSION.SDK_INT` and the SDK extension level) compared against the  
  reference data.
- **Tri-state semantics**: green when the device's extension level reaches or passes the stable release's;  
  red when it is lower, when the device is too old for the SDK extension feature (Android 10 and below), or  
  when there is no reference value. This item has no amber outcome.

#### HOME-04 Build ID

- **Question**: is this build as recent as the build the reference data knows for this Android release?
- **Techniques**: a public SDK API (`Build.ID`), the `ro.system.build.id` / `ro.vendor.build.id` /  
  `ro.odm.build.id` properties, compared against the reference data.
- **Tri-state semantics**: green when the build's date is at or after the first reference build of that  
  release; amber when the device runs the latest stable or preview release but the build's date is older; red  
  otherwise, and also red when there is no reference data.

#### HOME-05 Security patches (two rows)

- **Question**: is the platform security patch level, and the vendor one, at or after the reference level?
- **Techniques**: a public SDK API (`Build.VERSION.SECURITY_PATCH`) and the `ro.vendor.build.security_patch`  
  property, compared against the reference data.
- **Tri-state semantics** (each row): green when the device's level is at or after the reference level; amber  
  when it is lower but falls in the same year and month; red when it is lower and older than that, when the  
  value is empty, or when there is no reference value.

#### HOME-06 Performance class

- **Question**: which media performance class does the device declare?
- **Techniques**: a public SDK API (`Build.VERSION.MEDIA_PERFORMANCE_CLASS`).
- **Tri-state semantics**: green when the declared class equals the device's SDK level; amber when it is one  
  level below; red when it is lower than that, when no class is declared, or when the device is too old for the  
  field (Android 11 and below).

#### HOME-07 Kernel

- **Question**: is the Linux kernel at or after the version Android upstream ships for its major and minor  
  numbers?
- **Techniques**: the `os.version` system property compared against the reference data.
- **Tri-state semantics**: green when the kernel reaches or passes the supported version with the same major  
  and minor numbers; amber when it is lower with the same major and minor; red when no reference version  
  shares its major and minor, and also red when there is no reference data.

#### HOME-08 A/B updates

- **Question**: does the device use seamless A/B updates?
- **Techniques**: the `ro.build.ab_update`, `ro.boot.slot_suffix` and related virtual A/B properties.
- **Tri-state semantics**: green when A/B is enabled by any of the checked properties, and the row then names  
  the current slot and whether virtual A/B is retrofit; red when none of them says so. This item has no amber  
  outcome.

#### HOME-09 system-as-root (SAR)

- **Question**: is the system mounted as root, and by which layout?
- **Techniques**: the `ro.build.system_root_image` property and the mount table.
- **Tri-state semantics**: green when the layout is 2SI or a `/` mount; amber when it is legacy SAR or a  
  recovery SAR; red when the device is not system-as-root, and also red on Android 8 and below, where the  
  detection does not apply.

#### HOME-10 Dynamic partitions

- **Question**: does the device use dynamic partitions?
- **Techniques**: the `ro.boot.dynamic_partitions` and `ro.boot.dynamic_partitions_retrofit` properties.
- **Tri-state semantics**: green when dynamic partitions are enabled or were retrofitted; red when neither  
  holds. This item has no amber outcome.

#### HOME-11 Treble and GSI compatibility

- **Question**: does the device support Project Treble and a Generic System Image?
- **Techniques**: the `ro.treble.enabled` property and the vendor VINTF files on disk for the Treble row; a  
  shell command that reads the vendor namespace configuration for the GSI row.
- **Tri-state semantics** (two rows):
  - **Treble row**: green when Treble is enabled and a vendor manifest, manifest fragment or SKU manifest  
    exists; amber when Treble is enabled but only the legacy no-fragments layout or another layout is present;  
    red when Treble is disabled.
  - **GSI row**: green when the command reports the vendor namespace as compatible; amber when it reports it  
    as incompatible, or when it cannot be read while Treble is enabled; red when Treble is disabled or the  
    device is too old for the check (Android 8 and below).

#### HOME-12 DSU (Dynamic System Updates)

- **Question**: does the device support Dynamic System Updates?
- **Techniques**: the `persist.sys.fflag.override.settings_dynamic_system` and  
  `sys.fflag.override.settings_dynamic_system` properties.
- **Tri-state semantics**: green when either property enables DSU; red otherwise, where a disabled feature  
  shows as `Unknown` in the row. This item has no amber outcome.

#### HOME-13 Mainline (Google Play system updates)

- **Question**: is the installed Mainline module at or after the version Google publishes for this release?
- **Techniques**: the framework's `config_defaultModuleMetadataProvider` string resource, read through  
  `Resources.getSystem()`, then the metadata package's version name, compared against the reference data.
- **Tri-state semantics**: green when the installed module version reaches the published one; red when it is  
  lower, when the provider or its package cannot be read, and when there is no reference data. This item has no  
  amber outcome.

#### HOME-14 VNDK (vendor NDK)

- **Question**: does the device ship a built-in VNDK, and is it current?
- **Techniques**: the `ro.vndk.version` and `ro.vndk.lite` properties, compared against the reference data.
- **Tri-state semantics**: green when a VNDK version exists and is at or after the stable release's API level  
  (or the device runs the latest preview) and the build is not the Lite variant; amber when it is lower or the  
  build is Lite; red when no VNDK version exists, and also red when there is no reference data.

#### HOME-15 APEX (flattened)

- **Question**: does the device use APEX?
- **Techniques**: the `ro.apex.updatable` property and the mount table.
- **Tri-state semantics**: green when APEX is updatable; amber when only flattened APEX mounts are present and  
  APEX is not updatable; red when neither holds.

#### HOME-16 Developer options

- **Question**: are the developer options switched on?
- **Techniques**: a public SDK API (`Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`).
- **Tri-state semantics**: green when they are off; red when they are on. This item has no amber outcome.

#### HOME-17 ADB

- **Question**: is ADB debugging switched on?
- **Techniques**: a public SDK API (`Settings.Global.ADB_ENABLED`).
- **Tri-state semantics**: green when it is off; red when it is on. This item has no amber outcome.

#### HOME-18 ADB authentication

- **Question**: does ADB require key authentication?
- **Techniques**: the `ro.adb.secure` property.
- **Tri-state semantics**: green when authentication is enabled; red when it is not. This item has no amber  
  outcome.

#### HOME-19 Encryption

- **Question**: is the device storage encrypted?
- **Techniques**: a public SDK API (`DevicePolicyManager.storageEncryptionStatus`).
- **Tri-state semantics**: green when storage is encrypted (with the credential or the per-user key); amber  
  when it is encrypted with the default key and no credential is set; red when it is not encrypted, when  
  encryption is unsupported, and for any other status.

#### HOME-20 SELinux

- **Question**: is SELinux enforcing?
- **Techniques**: a shell command (`getenforce`).
- **Tri-state semantics**: green when the mode is enforcing, and also green when the command is denied in a  
  way that itself proves an enforcing policy; amber when it is permissive; red when it is disabled, and red  
  when the mode cannot be read.

#### HOME-21 Toybox

- **Question**: does the device ship Toybox, and how does its version compare with upstream?
- **Techniques**: a shell command (`toybox --version`), compared against the reference data.
- **Tri-state semantics**: green when the installed version reaches the reference stable version; amber when  
  it reaches the reference support version; red when it is lower, when no Toybox is found, or when there is no  
  reference data.

#### HOME-22 WebView implementation

- **Question**: which WebView implementation is in use, and is it current?
- **Techniques**: a reflective read of `WebViewUpdateService.getAllWebViewPackages`, a public SDK API  
  (`WebViewCompat.getCurrentWebViewPackage`), compared against the reference data.
- **Tri-state semantics**: green when the built-in provider reaches the reference stable version; amber when  
  only the current implementation reaches it; red otherwise, including when there is no reference value.

#### HOME-23 Outdated targetSdk apps

- **Question**: which preinstalled system apps target an older SDK than the device's, and how many?
- **Techniques**: a public SDK API (`PackageManager.getInstalledApplications`), filtered to preinstalled  
  system apps, and the `ro.product.first_api_level` property.
- **Tri-state semantics**: green when no preinstalled system app targets an older SDK and the device runs the  
  latest stable or preview release; amber when no such app exists but the device is not on the latest release;  
  red when at least one such app exists, and red when there is no reference data.

## Others (37 items)

Others rows carry no verdict: no dot, no color.

| id | Item | Question | Techniques | Notes |
|---|---|---|---|---|
| OTH-01 | Brand | which brand does the build declare? | public SDK API (`Build.BRAND`) | |
| OTH-02 | Manufacturer | which manufacturer does the build declare? | public SDK API (`Build.MANUFACTURER`) | |
| OTH-03 | Model | which model name does the build declare? | public SDK API (`Build.MODEL`) | |
| OTH-04 | Device | which device codename does the build declare? | public SDK API (`Build.DEVICE`) | |
| OTH-05 | Product | which product name does the build declare? | public SDK API (`Build.PRODUCT`) | |
| OTH-06 | Hardware | which hardware name does the build declare? | public SDK API (`Build.HARDWARE`) | |
| OTH-07 | Board | which board name does the build declare? | public SDK API (`Build.BOARD`) | |
| OTH-08 | SoC model | which SoC does the build declare? | public SDK API (`Build.SOC_MODEL`) | Android 12+ |
| OTH-09 | SoC manufacturer | which SoC vendor does the build declare? | public SDK API (`Build.SOC_MANUFACTURER`) | Android 12+ |
| OTH-10 | SKU | which SKU does the build declare? | public SDK API (`Build.SKU`) | Android 12+ |
| OTH-11 | Vendor SKU | which vendor SKU does the system property report? | a system property | Android 12+ |
| OTH-12 | ODM SKU | which ODM SKU does the build declare? | public SDK API (`Build.ODM_SKU`) | Android 12+ |
| OTH-13 | Binder state (the binder driver) | which binder protocol version does the driver report? | native code (JNI) reading the driver; an errno is reported as unsupported or unknown | |
| OTH-14 | Process bitness | is this app's process 32-bit or 64-bit? | public SDK API (`Process.is64Bit()`) | |
| OTH-15 | Process and VM architecture | which architecture does the JVM report? | the `os.arch` system property | |
| OTH-16 | CPU ABI (the build field) | which ABI does the build declare? | public SDK API (`Build.CPU_ABI`) | |
| OTH-17 | CPU ABI (the system property) | which ABI does the system property report? | a system property | |
| OTH-18 | Supported 32-bit ABIs | which 32-bit ABIs does the device support? | public SDK API (`Build.SUPPORTED_32_BIT_ABIS`) | |
| OTH-19 | Supported 64-bit ABIs | which 64-bit ABIs does the device support? | public SDK API (`Build.SUPPORTED_64_BIT_ABIS`) | an empty list shows as not supported |
| OTH-20 | Build user | which user built this build? | public SDK API (`Build.USER`) | |
| OTH-21 | Build host | which host built this build? | public SDK API (`Build.HOST`) | |
| OTH-22 | Build time | when was this build made? | public SDK API (`Build.TIME`), shown in the local timezone | |
| OTH-23 | Base OS | which base OS does the build declare? | public SDK API (`Build.VERSION.BASE_OS`) | |
| OTH-24 | Fingerprint | which build fingerprint does the build declare? | public SDK API (`Build.FINGERPRINT`) | |
| OTH-25 | Preview SDK fingerprint | which preview SDK fingerprint does the system property report? | a system property | Android 10+ |
| OTH-26 | Partition fingerprints | which fingerprint does each partition report? | one system property per partition | one row per partition whose fingerprint property is readable; a missing value drops that row |
| OTH-27 | Build Id | which build id does the build declare? | public SDK API (`Build.ID`) | |
| OTH-28 | Display | which display id does the build declare? | public SDK API (`Build.DISPLAY`) | |
| OTH-29 | Build type | which build type does the build declare? | public SDK API (`Build.TYPE`) | |
| OTH-30 | Build tags | which tags does the build declare? | public SDK API (`Build.TAGS`) | |
| OTH-31 | Incremental | which incremental version does the build declare? | public SDK API (`Build.VERSION.INCREMENTAL`) | |
| OTH-32 | Version codename | which codename does the build declare? | public SDK API (`Build.VERSION.CODENAME`) | on Android 13 and above preview builds, a developer-preview suffix is appended when the release or preview display differs from the codename |
| OTH-33 | Preview SDK int | which preview SDK level does the build declare? | public SDK API (`Build.VERSION.PREVIEW_SDK_INT`) | |
| OTH-34 | Default user agent | which user agent does the system WebView report? | public SDK API (`WebSettings.getDefaultUserAgent`) | |
| OTH-35 | Kernel version | which kernel version does the device run? | a shell command | |
| OTH-36 | Bootloader | which bootloader version does the build declare? | public SDK API (`Build.BOOTLOADER`) | |
| OTH-37 | Baseband version | which baseband version does the device report? | public SDK API (`Build.getRadioVersion()`) | |

## Prop (5 items)

Prop rows carry no verdict: no dot, no color. Each row's title is the key itself.

| id | Item | Question | Techniques | Notes |
|---|---|---|---|---|
| PROP-01 | System properties (`getprop`) | which system properties does the app process see? | a public SDK API (`System.getProperties()`) plus a reflective read of the `Properties.defaults` field | one row per property, sorted by key |
| PROP-02 | The `Settings.System` table | which entries does the system settings table hold? | the class's declared `String` fields, then a reflective `getString(ContentResolver, String)` read per key | one row per key |
| PROP-03 | The `Settings.Secure` table | which entries does the secure settings table hold? | the class's declared `String` fields, then a reflective `getString(ContentResolver, String)` read per key | one row per key |
| PROP-04 | The `Settings.Global` table | which entries does the global settings table hold? | the class's declared `String` fields, then a reflective `getString(ContentResolver, String)` read per key | one row per key |
| PROP-05 | build.prop | which entries does the `getprop` output carry? | a shell command (`getprop`) | one row per parsed `[key]: [value]` entry; a multi-line entry stays one row |

## Item template

When an item is added or adjusted, it carries these fields:

| Field | Meaning |
|---|---|
| **id** | a stable identifier for the item |
| **screen** | Home, Others or Prop |
| **title** | the item's name, as this catalog lists it; the user-visible string lives in the feature's `strings.xml` |
| **question** | what question the item answers, in plain words |
| **techniques** | the detection means the item uses, named with FR-9's vocabulary where it applies, plus the reference data of FR-16 where the item compares against it |
| **tri-state semantics** | what counts as supported, unsupported and unknown, and which color each gets, stated by the item's own semantics (FR-3, FR-15, NFR-7); an item that distinguishes only two outcomes says so |
