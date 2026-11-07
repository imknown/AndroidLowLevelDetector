package net.imknown.android.forefrontinfo.ui.base.detect

import android.provider.Settings
import net.imknown.android.forefrontinfo.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RuntimeEnvironment
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// Proof that Robolectric runs on the JUnit platform in this repo: the settings fake accepts
// staged values and the merged resources resolve. The per-entry detection unit tests stand on
// these two.
@ExtendWith(RobolectricExtension::class)
class RobolectricSpikeTest {

    // The settings fake accepts writes and reads back; entry tests stage values through it.
    @Test
    fun `settings global round trip works on the jvm`() {
        val context = RuntimeEnvironment.getApplication()

        Settings.Global.putInt(context.contentResolver, "spike_key", 1)

        assertEquals(1, Settings.Global.getInt(context.contentResolver, "spike_key"))
    }

    // Resource resolution goes through the merged app resources, which isIncludeAndroidResources enables.
    @Test
    fun `app resources resolve on the jvm`() {
        val context = RuntimeEnvironment.getApplication()

        val supported = context.getString(R.string.result_supported)

        assertTrue(supported.isNotEmpty())
    }
}
