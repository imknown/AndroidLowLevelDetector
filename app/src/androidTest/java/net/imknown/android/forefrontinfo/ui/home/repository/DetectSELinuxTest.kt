package net.imknown.android.forefrontinfo.ui.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.SELinuxState
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectSELinux against the real shell (spec 2026-10-25-22-16-01,
// subtask-04 batch 7): this entry runs getenforce, so per the means table it is
// instrumented-only; exact values are covered by each batch's golden snapshot comparison.
@RunWith(AndroidJUnit4::class)
class DetectSELinuxTest {

    private val repository by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication)
            .appGraph.homeRepository
    }

    @Test
    fun selinuxModeComesFromTheRealGetenforce() {
        val result = repository.detectSELinux()

        assertTrue("expected Found, got $result", result is DetectResult.Found)
        val state = (result as DetectResult.Found).value

        // The four buckets are exhaustive; the permission-denied-is-enforcing reading has no
        // device-side trigger, so any mode is a valid contract here.
        when (state.mode) {
            SELinuxState.Mode.Enforcing,
            SELinuxState.Mode.Permissive,
            SELinuxState.Mode.Disabled,
            SELinuxState.Mode.Unknown -> Unit
        }
    }
}
