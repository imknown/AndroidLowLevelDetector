package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.ToyboxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectToybox against the real shell and the bundled lld
// (spec 2026-10-25-22-16-01, subtask-04 batch 6): this entry runs a real shell command, so per
// the means table it is instrumented-only.
@RunWith(AndroidJUnit4::class)
class DetectToyboxTest {

    private val app by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication
    }
    private val repository by lazy { app.appGraph.homeRepository }

    @Test
    fun toyboxWiresTheRealVersionAndTheLldReferences() {
        val lld = repository.getAssetLld(app.assets)
        assertNotNull("the bundled lld must load", lld)

        val result = repository.detectToybox(lld)

        assertTrue("expected Found, got $result", result is DetectResult.Found)
        val state = (result as DetectResult.Found).value

        // The three lld reference versions ride verbatim (absent stays absent).
        assertEquals(lld?.toybox?.stable?.version, state.stableVersion)
        assertEquals(lld?.toybox?.support?.version, state.supportVersion)
        assertEquals(lld?.toybox?.master?.version, state.masterVersion)
        // toybox exists on the test device (API 34): the command must not report failure.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue(state.read !is ToyboxState.ToyboxRead.CommandFailed)
        }
    }
}
