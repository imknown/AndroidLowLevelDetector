package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.ui.home.model.ToyboxState
import org.junit.Assert.assertEquals
import org.junit.Test

// The domain contract of toyboxMatch: the version ladder runs only on a real read; a build
// at or above the lld's stable version is met, one at or above only the support version is
// lagging, below both is none, and a no-output or failed command is none outright.
class DetectToyboxTest {

    @Test
    fun `a toybox at the lld stable version is met`() {
        assertEquals(
            ToyboxState.Match.Met,
            toyboxMatch(ToyboxState.ToyboxRead.Version("toybox 0.9.0"), "0.9.0", "0.8.0")
        )
    }

    @Test
    fun `a toybox above the support version but below stable is lagging`() {
        assertEquals(
            ToyboxState.Match.Lagging,
            toyboxMatch(ToyboxState.ToyboxRead.Version("toybox 0.8.5"), "0.9.0", "0.8.0")
        )
    }

    @Test
    fun `a toybox below both lld versions is none`() {
        assertEquals(
            ToyboxState.Match.None,
            toyboxMatch(ToyboxState.ToyboxRead.Version("toybox 0.7.0"), "0.9.0", "0.8.0")
        )
    }

    @Test
    fun `a command without output or a failed command is none`() {
        assertEquals(
            ToyboxState.Match.None,
            toyboxMatch(ToyboxState.ToyboxRead.NoOutput, "0.9.0", "0.8.0")
        )
        assertEquals(
            ToyboxState.Match.None,
            toyboxMatch(ToyboxState.ToyboxRead.CommandFailed, "0.9.0", "0.8.0")
        )
    }
}
