package net.imknown.android.forefrontinfo.ui.home.mapper

import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The pure translation contract: the three branches render their own colors. No try here, the
// detection engine owns the per-entry guard (its contract lives in DetectionEngineTest).
// Asserts row shape and colors only, never the exact display text.
@RunWith(RobolectricTestRunner::class)
class HomeModelMapperTest {

    private val mapper by lazy { HomeModelMapper(MyApplication.instance) }

    @Test
    fun `failed result renders the failure row`() {
        val row = mapper.toDeveloperOptionsModel(DetectResult.Failed(IllegalStateException("boom")))

        assertEquals(StatusColor.CRITICAL, row.color)
    }

    @Test
    fun `found true and false map to the two state colors`() {
        val disabled = mapper.toDeveloperOptionsModel(DetectResult.Found(true))
        val enabled = mapper.toDeveloperOptionsModel(DetectResult.Found(false))

        assertEquals(StatusColor.NO_PROBLEM, disabled.color)
        assertEquals(StatusColor.CRITICAL, enabled.color)
    }

    @Test
    fun `security patch failure rows keep the two row titles`() {
        val rows = mapper.toSecurityPatchFailureRows(IllegalStateException("probe"))

        assertEquals(2, rows.size)
        assertEquals(
            listOf(
                MyModelTitle.Res(R.string.security_patch_level_title),
                MyModelTitle.Res(R.string.vendor_security_patch_level_title)
            ),
            rows.map { it.title }
        )
        assertEquals(listOf(StatusColor.CRITICAL, StatusColor.CRITICAL), rows.map { it.color })
    }

    @Test
    fun `treble and gsi failure rows keep the two row titles`() {
        val rows = mapper.toTrebleAndGsiFailureRows(IllegalStateException("probe"))

        assertEquals(2, rows.size)
        assertEquals(
            listOf(
                MyModelTitle.Res(R.string.treble_status_title),
                MyModelTitle.Res(R.string.gsi_status_title)
            ),
            rows.map { it.title }
        )
        assertEquals(listOf(StatusColor.CRITICAL, StatusColor.CRITICAL), rows.map { it.color })
    }

    // The order switch is a render parameter for this one row: the mapper sorts the repository's
    // list and picks the detail's format from it, so switching never re-detects.
    @Test
    fun `the outdated target row lists packages by name when the switch is on`() {
        val state = OutdatedTargetSdkVersionApkState(
            firstApiLevel = "31",
            outdatedApks = listOf(
                OutdatedTargetSdkVersionApkState.OutdatedApk("zzz.pkg", 33),
                OutdatedTargetSdkVersionApkState.OutdatedApk("aaa.pkg", 30)
            ),
            match = OutdatedTargetSdkVersionApkState.Match.None
        )

        val detail = mapper.toOutdatedTargetSdkVersionApkModel(DetectResult.Found(state), true).detail

        assertTrue(
            "package-name order expects aaa.pkg before zzz.pkg, got: $detail",
            detail.indexOf("aaa.pkg") < detail.indexOf("zzz.pkg")
        )
    }

    @Test
    fun `the outdated target row lists packages by target sdk when the switch is off`() {
        val state = OutdatedTargetSdkVersionApkState(
            firstApiLevel = "31",
            outdatedApks = listOf(
                OutdatedTargetSdkVersionApkState.OutdatedApk("aaa.pkg", 33),
                OutdatedTargetSdkVersionApkState.OutdatedApk("zzz.pkg", 30)
            ),
            match = OutdatedTargetSdkVersionApkState.Match.None
        )

        val detail = mapper.toOutdatedTargetSdkVersionApkModel(DetectResult.Found(state), false).detail

        assertTrue(
            "target-sdk order expects zzz.pkg before aaa.pkg, got: $detail",
            detail.indexOf("zzz.pkg") < detail.indexOf("aaa.pkg")
        )
    }
}
