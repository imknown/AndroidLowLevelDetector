package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectSar against the real mount table (spec 2026-10-25-22-16-01,
// subtask-04 batch 6): this entry reads /proc/mounts, so per the means table it is
// instrumented-only; exact values are covered by each batch's golden snapshot comparison.
@RunWith(AndroidJUnit4::class)
class DetectSarTest {

    private val repository by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication)
            .appGraph.homeRepository
    }

    @Test
    fun sarFactsComeFromTheRealMountTable() {
        val result = repository.detectSar(repository.getMounts())

        assertTrue("expected Found, got $result", result is DetectResult.Found)
        val state = (result as DetectResult.Found).value

        // From Q on the SDK arm alone decides SAR, so the test device (API 34) always reports it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue(state.supported)
        }
        // The type rides with supported: a supported SAR always carries one, an unsupported one
        // never does.
        assertEquals(state.supported, state.type != null)
    }
}
