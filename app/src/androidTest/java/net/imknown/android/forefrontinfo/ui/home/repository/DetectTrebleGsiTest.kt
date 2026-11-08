package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.GsiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of detectTrebleAndGsiCompatibility against the real filesystem and shell
// (spec 2026-10-25-22-16-01, subtask-04 batch 6): treble probes real vintf paths, GSI runs a
// real shell command, so per the means table both are instrumented-only.
@RunWith(AndroidJUnit4::class)
class DetectTrebleGsiTest {

    private val repository by lazy {
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication)
            .appGraph.homeRepository
    }

    @Test
    fun trebleAndGsiAgreeOnThisDevice() {
        val (trebleResult, gsiResult) = repository.detectTrebleAndGsiCompatibility()

        assertTrue("expected treble Found, got $trebleResult", trebleResult is DetectResult.Found)
        assertTrue("expected gsi Found, got $gsiResult", gsiResult is DetectResult.Found)
        val treble = (trebleResult as DetectResult.Found).value
        val gsi = (gsiResult as DetectResult.Found).value

        // The vintf layout is only probed while treble is on: null iff disabled.
        assertEquals(treble.enabled, treble.vintf != null)
        // From P the GSI command actually runs, so its not-supported branch is exactly the
        // treble-unsupported case (the old color bridge, now a fact-level contract).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertEquals(treble.enabled, gsi.compatibility != GsiState.Compatibility.NotSupported)
        }
    }
}
