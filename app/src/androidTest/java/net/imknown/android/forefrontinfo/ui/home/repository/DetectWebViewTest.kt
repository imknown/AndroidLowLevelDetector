package net.imknown.android.forefrontinfo.ui.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.WebViewState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectWebView against the real package manager and the hidden
// WebViewUpdateService (spec 2026-10-25-22-16-01, subtask-04 batch 7): per the means table this
// entry is instrumented-only; exact values are covered by each batch's golden snapshot
// comparison.
@RunWith(AndroidJUnit4::class)
class DetectWebViewTest {

    private val app by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication
    }
    private val repository by lazy { app.appGraph.homeRepository }

    @Test
    fun webViewFactsComeFromTheRealPackageManager() {
        val lld = repository.getAssetLld(app.assets)
        assertNotNull("the bundled lld must load", lld)

        val result = repository.detectWebView(lld)

        assertTrue("expected Found, got $result", result is DetectResult.Found)
        val state = (result as DetectResult.Found).value

        // The match buckets are exhaustive by type; which one applies depends on the device's
        // installed versions against the lld reference. An empty candidate list and a null
        // implementation are legal Found values: the repository degrades to them when the
        // hidden WebViewUpdateService reflection or the current-webview lookup fails (FR-9 AC1,
        // NFR-1), so no existence assertion goes here.
        when (state.match) {
            WebViewState.Match.Met,
            WebViewState.Match.Lagging,
            WebViewState.Match.None -> Unit
        }
    }
}
