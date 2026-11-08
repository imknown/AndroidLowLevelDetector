package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.SecurityPatchState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.shadows.ShadowSystemProperties
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectSecurityPatches: two rows ride in the system-then-vendor order,
// each with its own result; the vendor value is a raw property read (an unread property
// carries the placeholder it produced, until the #42 rework) while the system value is the
// platform's SECURITY_PATCH, which the Robolectric environment does not let tests stage.
@ExtendWith(RobolectricExtension::class)
class DetectSecurityPatchesTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `a missing lld means both rows unknown with their values carried`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, "2026-11-05")

        assertEquals(
            listOf(
                DetectResult.Found(
                    SecurityPatchState(
                        patch = Build.VERSION.SECURITY_PATCH,
                        lldPatchLevel = null,
                        match = SecurityPatchState.Match.Unknown
                    )
                ),
                DetectResult.Found(
                    SecurityPatchState(
                        patch = "2026-11-05",
                        lldPatchLevel = null,
                        match = SecurityPatchState.Match.Unknown
                    )
                )
            ),
            repository.detectSecurityPatches(null)
        )
    }

    @Test
    fun `a vendor patch past the lld level is met`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, "2026-12-01")

        val vendor = (repository.detectSecurityPatches(lldForTests())[1] as DetectResult.Found).value

        assertEquals("2026-12-01", vendor.patch)
        assertEquals("2026-11-01", vendor.lldPatchLevel)
        assertEquals(SecurityPatchState.Match.Met, vendor.match)
    }

    @Test
    fun `a vendor patch in the lld level month but behind its day is lagging`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, "2026-11-01")
        val lld = lldForTests(android = lldForTests().android.copy(securityPatchLevel = "2026-11-15"))

        val vendor = (repository.detectSecurityPatches(lld)[1] as DetectResult.Found).value

        assertEquals("2026-11-15", vendor.lldPatchLevel)
        assertEquals(SecurityPatchState.Match.Lagging, vendor.match)
    }

    @Test
    fun `an unread vendor property carries its placeholder raw and is none`() {
        val vendor = (repository.detectSecurityPatches(lldForTests())[1] as DetectResult.Found).value

        assertEquals(MyApplication.getMyString(R.string.build_not_filled), vendor.patch)
        assertEquals(SecurityPatchState.Match.None, vendor.match)
    }
}
