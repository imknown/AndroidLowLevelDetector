package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.SdkExtensionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectSdkExtension on a level that declares extensions: the current
// value comes from the platform extension table (its absolute value is shadow-dependent and
// not asserted), the stable value from the lld, and the meets judgment is the repository's.
@ExtendWith(RobolectricExtension::class)
class DetectSdkExtensionTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun lldWithStableExtension(extension: Int): Lld {
        val base = lldForTests()
        return base.copy(
            android = base.android.copy(stable = base.android.stable.copy(extension = extension))
        )
    }

    @Test
    fun `any current value meets a zero stable extension`() {
        // EXTENSION_NONE (0) as the stable bar: every current value is at or above it.
        val lld = lldWithStableExtension(0)

        val state = (repository.detectSdkExtension(lld) as DetectResult.Found).value

        assertEquals(true, state.meetsStable)
        assertEquals(0, state.stableExtension)
        assertEquals(true, state.currentExtension != null)
    }

    @Test
    fun `an unreachable stable bar is not met`() {
        val lld = lldWithStableExtension(Int.MAX_VALUE)

        val state = (repository.detectSdkExtension(lld) as DetectResult.Found).value

        assertEquals(false, state.meetsStable)
        assertEquals(Int.MAX_VALUE, state.stableExtension)
        assertEquals(true, state.currentExtension != null)
    }
}
