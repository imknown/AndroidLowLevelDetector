package net.imknown.android.forefrontinfo.ui.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectApex against the real mount table and property
// (spec 2026-10-25-22-16-01, subtask-04 batch 6): this entry reads /proc/mounts and
// ro.apex.updatable, so per the means table it is instrumented-only; which branch the test
// device takes is not pinned, exact values are covered by the golden snapshot comparison.
@RunWith(AndroidJUnit4::class)
class DetectApexTest {

    private val repository by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication)
            .appGraph.homeRepository
    }

    @Test
    fun apexFactsComeFromTheRealMountTableAndProperty() {
        val result = repository.detectApex(repository.getMounts())

        assertTrue("expected Found, got $result", result is DetectResult.Found)
    }
}
