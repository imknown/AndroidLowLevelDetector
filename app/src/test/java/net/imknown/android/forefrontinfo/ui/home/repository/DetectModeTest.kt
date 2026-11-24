package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.LldFailure
import net.imknown.android.forefrontinfo.ui.home.model.LldFailureReason
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import net.imknown.android.forefrontinfo.ui.home.model.LldStatusState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The domain contract of detectMode: the payload version passes through (null when the lld is
// missing), the source is carried, and the failures arrive as reasons in fetch order (the mapper,
// not the repository, turns them into text).
@RunWith(RobolectricTestRunner::class)
class DetectModeTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `an online payload carries its version and source`() {
        val lld = lldForTests(version = "2026-11-07T10:00:00+08:00")

        assertEquals(
            DetectResult.Found(
                LldStatusState(version = "2026-11-07T10:00:00+08:00", source = LldSource.ONLINE, failures = emptyList())
            ),
            repository.detectMode(lld, emptyList(), LldSource.ONLINE)
        )
    }

    @Test
    fun `a missing lld carries a null version`() {
        assertEquals(
            DetectResult.Found(
                LldStatusState(version = null, source = LldSource.OFFLINE, failures = emptyList())
            ),
            repository.detectMode(null, emptyList(), LldSource.OFFLINE)
        )
    }

    @Test
    fun `failures keep the order the chain reported them in`() {
        val lld = lldForTests()
        val failures = listOf(
            LldFailure(LldFailureReason.Fetch, "first failure"),
            LldFailure(LldFailureReason.Save, "second failure")
        )

        assertEquals(
            DetectResult.Found(
                LldStatusState(
                    version = "2026-11-07T10:00:00+08:00",
                    source = LldSource.OFFLINE,
                    failures = failures
                )
            ),
            repository.detectMode(lld, failures, LldSource.OFFLINE)
        )
    }
}
