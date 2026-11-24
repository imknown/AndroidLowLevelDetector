package net.imknown.android.forefrontinfo.ui.base.detect

import net.imknown.android.forefrontinfo.ui.home.model.HomeEntryId
import net.imknown.android.forefrontinfo.ui.others.model.OthersEntryId
import net.imknown.android.forefrontinfo.ui.prop.model.PropEntryId
import org.junit.Assert.assertEquals
import org.junit.Test

// Locks the three pages' entry id sequences against the row order of the detection-entry
// catalog (docs/requirements-cn/04-检测条目目录.md). The enum declaration order is the display
// order, the view models' detector lists follow it, and the catalog rows carry the same ids in
// the same order; a reorder, a rename or a dropped entry turns this red until the catalog rows
// and the numbering move with it. The expected lists are the catalog's rows written out here on
// purpose: the catalog is prose, this test is the machine-checkable copy of it.
class DetectionEntryOrderTest {

    @Test
    fun `home entries keep the catalog row order`() {
        assertEquals(
            listOf(
                "home-detect-mode",
                "home-android-version",
                "home-sdk-extension-version",
                "home-build-id",
                "home-security-patch",
                "home-performance-class",
                "home-kernel",
                "home-ab-update",
                "home-sar",
                "home-dynamic-partitions",
                "home-treble-gsi",
                "home-dsu",
                "home-mainline",
                "home-vndk",
                "home-apex",
                "home-developer-options",
                "home-adb",
                "home-adb-authentication",
                "home-encryption",
                "home-selinux",
                "home-toybox",
                "home-webview",
                "home-outdated-target-sdk-apk"
            ),
            HomeEntryId.entries.map { it.id }
        )
    }

    @Test
    fun `others entries keep the catalog row order`() {
        assertEquals(
            listOf(
                "oth-brand",
                "oth-manufacturer",
                "oth-model",
                "oth-device",
                "oth-product",
                "oth-hardware",
                "oth-board",
                "oth-soc-model",
                "oth-soc-manufacturer",
                "oth-sku",
                "oth-vendor-sku",
                "oth-odm-sku",
                "oth-binder-status",
                "oth-process-bit",
                "oth-process-architecture",
                "oth-cpu-abi-build",
                "oth-cpu-abi-property",
                "oth-supported-32-bit-abis",
                "oth-supported-64-bit-abis",
                "oth-build-user",
                "oth-build-host",
                "oth-build-time",
                "oth-base-os",
                "oth-fingerprint",
                "oth-preview-sdk-fingerprint",
                "oth-partition-fingerprints",
                "oth-build-id",
                "oth-display",
                "oth-build-type",
                "oth-build-tags",
                "oth-incremental",
                "oth-codename",
                "oth-preview-sdk-int",
                "oth-default-user-agent",
                "oth-kernel-version",
                "oth-bootloader",
                "oth-radio-version"
            ),
            OthersEntryId.entries.map { it.id }
        )
    }

    @Test
    fun `prop entries keep the catalog row order`() {
        assertEquals(
            listOf(
                "prop-system-prop",
                "prop-settings-system",
                "prop-settings-secure",
                "prop-settings-global",
                "prop-build-prop"
            ),
            PropEntryId.entries.map { it.id }
        )
    }
}
