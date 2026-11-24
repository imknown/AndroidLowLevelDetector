package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.VndkState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties

// The domain contract of detectVndk: the built-in VNDK version uses the absence-aware read
// (null when absent or unread), the Lite flag is only read when a version exists, and the
// match compares the version against the lld's stable API (string comparison, as before).
@RunWith(RobolectricTestRunner::class)
class DetectVndkTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `an absent vndk version is none`() {
        assertEquals(
            DetectResult.Found(VndkState(vndkVersion = null, hasVndkLite = false, match = VndkState.Match.None)),
            repository.detectVndk(lldForTests())
        )
    }

    @Test
    fun `a version reaching the lld stable api without lite is met`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VNDK_VERSION, "36")
        val base = lldForTests().android
        val lld = lldForTests(android = base.copy(stable = base.stable.copy(api = "36")))

        assertEquals(
            DetectResult.Found(VndkState(vndkVersion = "36", hasVndkLite = false, match = VndkState.Match.Met)),
            repository.detectVndk(lld)
        )
    }

    @Test
    fun `a version behind the lld stable api is lagging`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VNDK_VERSION, "33")

        assertEquals(
            DetectResult.Found(VndkState(vndkVersion = "33", hasVndkLite = false, match = VndkState.Match.Lagging)),
            repository.detectVndk(lldForTests())
        )
    }

    @Test
    fun `the lite variant holds a reaching version back to lagging`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VNDK_VERSION, "36")
        ShadowSystemProperties.override(AndroidDataSource.PROP_VNDK_LITE, "true")
        val base = lldForTests().android
        val lld = lldForTests(android = base.copy(stable = base.stable.copy(api = "36")))

        assertEquals(
            DetectResult.Found(VndkState(vndkVersion = "36", hasVndkLite = true, match = VndkState.Match.Lagging)),
            repository.detectVndk(lld)
        )
    }

    @Test
    fun `a missing lld is unknown with the version carried`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VNDK_VERSION, "33")

        assertEquals(
            DetectResult.Found(VndkState(vndkVersion = "33", hasVndkLite = false, match = VndkState.Match.Unknown)),
            repository.detectVndk(null)
        )
    }
}
