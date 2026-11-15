package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState.Match
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of outdatedTargetSdkApkMatch: a non-empty outdated list is none (the
// rows carry the information), an empty device with no lld is unknown, an empty device on
// the latest (stable or preview) Android is met, and an empty device below both lld bars is
// lagging.
@ExtendWith(RobolectricExtension::class)
class DetectOutdatedTargetSdkTest {

    @Test
    fun `an outdated apk present is none regardless of the channel`() {
        val apk = OutdatedTargetSdkVersionApkState.OutdatedApk("com.example.old", 29)

        assertEquals(Match.None, outdatedTargetSdkApkMatch(listOf(apk), lldForTests()))
    }

    @Test
    fun `an empty device with no lld is unknown`() {
        assertEquals(Match.Unknown, outdatedTargetSdkApkMatch(emptyList(), null))
    }

    @Test
    fun `an empty device on the latest stable is met`() {
        assertEquals(Match.Met, outdatedTargetSdkApkMatch(emptyList(), lldForTests()))
    }

    @Test
    fun `an empty device below both lld bars is lagging`() {
        val base = lldForTests().android
        val lld = lldForTests(android = base.copy(stable = base.stable.copy(api = "99")))

        assertEquals(Match.Lagging, outdatedTargetSdkApkMatch(emptyList(), lld))
    }
}
