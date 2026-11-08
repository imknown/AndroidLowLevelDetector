package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.MainlineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectMainline: the provider and version nullability follows the
// framework config lookup (the Robolectric environment decides whether the internal resource
// resolves; either way no provider package is installed here), so the tests pin the match
// judgment and the carried lld date, not the environment's provider value.
@ExtendWith(RobolectricExtension::class)
class DetectMainlineTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `a missing lld is unknown`() {
        val state = (repository.detectMainline(null) as DetectResult.Found).value

        assertEquals(MainlineState.Match.Unknown, state.match)
        assertNull(state.latestGooglePlaySystemUpdates)
    }

    @Test
    fun `an lld date with no installed provider module is none`() {
        val state = (repository.detectMainline(lldForTests()) as DetectResult.Found).value

        assertEquals(MainlineState.Match.None, state.match)
        assertEquals("2026-11-01", state.latestGooglePlaySystemUpdates)
    }
}
