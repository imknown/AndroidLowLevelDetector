package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.BuildIdState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectBuildId: the device's own Build.ID and the three partition ids
// ride raw (an unread property carries the placeholder it produced, until the #42 rework); the
// freshness judgment compares the CTS-style date segment against the lld's first detail, with
// the preview-channel offset added on the preview side.
@ExtendWith(RobolectricExtension::class)
class DetectBuildIdTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun stagePartitionBuildIds(system: String, vendor: String = system, odm: String = system) {
        ShadowSystemProperties.override(AndroidDataSource.PROP_RO_SYSTEM_BUILD_ID, system)
        ShadowSystemProperties.override(AndroidDataSource.PROP_RO_VENDOR_BUILD_ID, vendor)
        ShadowSystemProperties.override(AndroidDataSource.PROP_RO_ODM_BUILD_ID, odm)
    }

    @Test
    fun `a missing lld means unknown with the device facts carried`() {
        ShadowBuild.setId("BP1.250705.001")
        stagePartitionBuildIds("BP1.250601.001", "BP1.250501.001", "BP1.250401.001")

        assertEquals(
            DetectResult.Found(
                BuildIdState(
                    buildId = "BP1.250705.001",
                    systemBuildId = "BP1.250601.001",
                    vendorBuildId = "BP1.250501.001",
                    odmBuildId = "BP1.250401.001",
                    lldBuildVersion = null,
                    lldDetails = null,
                    match = BuildIdState.Match.Unknown
                )
            ),
            repository.detectBuildId(null)
        )
    }

    @Test
    fun `a build date at the lld first detail is met`() {
        ShadowBuild.setId("BP1.250705.001")
        stagePartitionBuildIds("BP1.250705.001")

        val state = (repository.detectBuildId(lldForTests()) as DetectResult.Found).value

        assertEquals(BuildIdState.Match.Met, state.match)
        assertEquals("BP31.250705", state.lldBuildVersion)
        assertEquals(listOf(BuildIdState.BuildDetail("BP31.250705", "1")), state.lldDetails)
    }

    @Test
    fun `a build date behind the lld first detail on the latest stable is lagging`() {
        ShadowBuild.setId("BP1.250601.001")
        stagePartitionBuildIds("BP1.250601.001")

        val state = (repository.detectBuildId(lldForTests()) as DetectResult.Found).value

        assertEquals(BuildIdState.Match.Lagging, state.match)
    }

    @Test
    fun `the preview channel offset lifts an earlier build to met`() {
        ShadowBuild.setId("BP1.250601.001")
        ShadowBuild.setVersionCodename("Baklava")
        stagePartitionBuildIds("BP1.250601.001")

        val state = (repository.detectBuildId(lldForTests()) as DetectResult.Found).value

        assertEquals(BuildIdState.Match.Met, state.match)
    }

    @Test
    fun `a device below both lld bars is outdated`() {
        ShadowBuild.setId("BP1.240101.001")
        stagePartitionBuildIds("BP1.240101.001")
        val base = lldForTests().android
        val lld = lldForTests(android = base.copy(stable = base.stable.copy(api = "99")))

        val state = (repository.detectBuildId(lld) as DetectResult.Found).value

        assertEquals(BuildIdState.Match.Outdated, state.match)
    }
}
