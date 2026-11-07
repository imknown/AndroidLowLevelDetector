package net.imknown.android.forefrontinfo.ui.home.repository

import android.provider.Settings
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectDeveloperOptions (2a per-entry test, Robolectric as the primary
// means): asserts DetectResult only, never display text. The repository instance comes from the
// graph (AppGraph.homeRepository, the production wiring); lazy defers the lookup until the
// extension has built the application.
@ExtendWith(RobolectricExtension::class)
class DetectDeveloperOptionsTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun putDeveloperOptions(value: Int) {
        Settings.Global.putInt(
            MyApplication.instance.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            value
        )
    }

    @Test
    fun `disabled developer options are found as true`() {
        putDeveloperOptions(AndroidDataSource.SETTINGS_DISABLED)

        assertEquals(DetectResult.Found(true), repository.detectDeveloperOptions())
    }

    @Test
    fun `enabled developer options are found as false`() {
        putDeveloperOptions(AndroidDataSource.SETTINGS_ENABLED)

        assertEquals(DetectResult.Found(false), repository.detectDeveloperOptions())
    }

    @Test
    fun `absent setting falls back to disabled`() {
        putDeveloperOptions(AndroidDataSource.SETTINGS_ENABLED)
        Settings.Global.putString(
            MyApplication.instance.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            null
        )

        assertEquals(DetectResult.Found(true), repository.detectDeveloperOptions())
    }
}
