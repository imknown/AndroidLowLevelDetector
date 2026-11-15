package net.imknown.android.forefrontinfo.ui.others.model

import net.imknown.android.forefrontinfo.ui.base.detect.Detector
import net.imknown.android.forefrontinfo.ui.base.detect.rawDetector
import net.imknown.android.forefrontinfo.ui.base.list.MyModel

/**
 * The Others detection entries' stable ids. Declaration order is the display order the Others
 * list assembles (the view model's detector list follows it) and matches the row order of the
 * detection-entry catalog; the [id] strings are the code-facing join key until the catalog
 * carries them too. The rows are plain information: no three-state results, no colors.
 */
enum class OthersEntryId(val id: String) {
    OTH_BRAND("oth-brand"),
    OTH_MANUFACTURER("oth-manufacturer"),
    OTH_MODEL("oth-model"),
    OTH_DEVICE("oth-device"),
    OTH_PRODUCT("oth-product"),
    OTH_HARDWARE("oth-hardware"),
    OTH_BOARD("oth-board"),
    OTH_SOC_MODEL("oth-soc-model"),
    OTH_SOC_MANUFACTURER("oth-soc-manufacturer"),
    OTH_SKU("oth-sku"),
    OTH_VENDOR_SKU("oth-vendor-sku"),
    OTH_ODM_SKU("oth-odm-sku"),
    OTH_BINDER_STATUS("oth-binder-status"),
    OTH_PROCESS_BIT("oth-process-bit"),
    OTH_PROCESS_ARCHITECTURE("oth-process-architecture"),
    OTH_CPU_ABI_BUILD("oth-cpu-abi-build"),
    OTH_CPU_ABI_PROPERTY("oth-cpu-abi-property"),
    OTH_SUPPORTED_32_BIT_ABIS("oth-supported-32-bit-abis"),
    OTH_SUPPORTED_64_BIT_ABIS("oth-supported-64-bit-abis"),
    OTH_BUILD_USER("oth-build-user"),
    OTH_BUILD_HOST("oth-build-host"),
    OTH_BUILD_TIME("oth-build-time"),
    OTH_BASE_OS("oth-base-os"),
    OTH_FINGERPRINT("oth-fingerprint"),
    OTH_PREVIEW_SDK_FINGERPRINT("oth-preview-sdk-fingerprint"),
    OTH_PARTITION_FINGERPRINTS("oth-partition-fingerprints"),
    OTH_BUILD_ID("oth-build-id"),
    OTH_DISPLAY("oth-display"),
    OTH_BUILD_TYPE("oth-build-type"),
    OTH_BUILD_TAGS("oth-build-tags"),
    OTH_INCREMENTAL("oth-incremental"),
    OTH_CODENAME("oth-codename"),
    OTH_PREVIEW_SDK_INT("oth-preview-sdk-int"),
    OTH_DEFAULT_USER_AGENT("oth-default-user-agent"),
    OTH_KERNEL_VERSION("oth-kernel-version"),
    OTH_BOOTLOADER("oth-bootloader"),
    OTH_RADIO_VERSION("oth-radio-version")
}

/**
 * [Detector] factory keyed by [OthersEntryId]: the overload converts the enum to its id string
 * so the generic engine stays page-agnostic. The entries render rows directly (the repositories
 * guard themselves), so there is no [Detector.failureRows] fallback: a chain exception keeps
 * rethrowing, same as before the engine. [precondition] declares the platform gates.
 */
fun rawDetector(
    id: OthersEntryId,
    precondition: (() -> Boolean)? = null,
    run: () -> List<MyModel>
): Detector = rawDetector(id.id, run = run, precondition = precondition)
