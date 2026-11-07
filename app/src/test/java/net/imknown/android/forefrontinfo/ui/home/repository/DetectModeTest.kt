package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import net.imknown.android.forefrontinfo.ui.home.model.LldStatusState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectMode: the payload version passes through (null when the lld is
// missing), the source is carried, and null error entries are filtered out.
@ExtendWith(RobolectricExtension::class)
class DetectModeTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `an online payload carries its version and source`() {
        val lld = lldForTests(version = "2026-11-07T10:00:00+08:00")

        assertEquals(
            DetectResult.Found(
                LldStatusState(version = "2026-11-07T10:00:00+08:00", source = LldSource.ONLINE, errors = emptyList())
            ),
            repository.detectMode(lld, listOf(null, null), LldSource.ONLINE)
        )
    }

    @Test
    fun `a missing lld carries a null version`() {
        assertEquals(
            DetectResult.Found(
                LldStatusState(version = null, source = LldSource.OFFLINE, errors = emptyList())
            ),
            repository.detectMode(null, listOf(null), LldSource.OFFLINE)
        )
    }

    @Test
    fun `null error entries are filtered out in order`() {
        val lld = lldForTests()

        assertEquals(
            DetectResult.Found(
                LldStatusState(
                    version = "2026-11-07T10:00:00+08:00",
                    source = LldSource.OFFLINE,
                    errors = listOf("first failure", "second failure")
                )
            ),
            repository.detectMode(lld, listOf("first failure", null, "second failure", null), LldSource.OFFLINE)
        )
    }
}
