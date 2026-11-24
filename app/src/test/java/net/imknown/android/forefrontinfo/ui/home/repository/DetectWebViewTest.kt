package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.ui.home.model.WebViewState
import org.junit.Assert.assertEquals
import org.junit.Test

// The domain contract of webViewMatch: the built-in providers' highest version at or above
// the lld stable version is met; otherwise the implementing package reaching the stable
// version is lagging (Version(null) compares as 0.0, so an absent implementer never does);
// everything else is none.
class DetectWebViewTest {

    @Test
    fun `a built-in provider at the lld stable version is met`() {
        assertEquals(WebViewState.Match.Met, webViewMatch("140.0", null, "140.0"))
    }

    @Test
    fun `an implementing package at the lld stable with a lower built-in is lagging`() {
        assertEquals(WebViewState.Match.Lagging, webViewMatch("130.0", "141.0", "140.0"))
    }

    @Test
    fun `nothing reaching the lld stable version is none`() {
        assertEquals(WebViewState.Match.None, webViewMatch("130.0", "139.0", "140.0"))
    }

    @Test
    fun `an absent implementing package compares as zero and is none`() {
        assertEquals(WebViewState.Match.None, webViewMatch("130.0", null, "140.0"))
    }

    @Test
    fun `a missing lld stable version is none`() {
        assertEquals(WebViewState.Match.None, webViewMatch("150.0", "150.0", null))
    }
}
