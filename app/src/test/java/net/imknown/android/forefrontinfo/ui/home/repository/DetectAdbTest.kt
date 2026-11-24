package net.imknown.android.forefrontinfo.ui.home.repository

import android.provider.Settings
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The domain contract of detectAdb (detectDeveloperOptions' twin): asserts DetectResult only.
@RunWith(RobolectricTestRunner::class)
class DetectAdbTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun putAdb(value: Int) {
        Settings.Global.putInt(
            MyApplication.instance.contentResolver,
            Settings.Global.ADB_ENABLED,
            value
        )
    }

    @Test
    fun `disabled adb debugging is found as true`() {
        putAdb(AndroidDataSource.SETTINGS_DISABLED)

        assertEquals(DetectResult.Found(true), repository.detectAdb())
    }

    @Test
    fun `enabled adb debugging is found as false`() {
        putAdb(AndroidDataSource.SETTINGS_ENABLED)

        assertEquals(DetectResult.Found(false), repository.detectAdb())
    }

    @Test
    fun `absent setting falls back to disabled`() {
        putAdb(AndroidDataSource.SETTINGS_ENABLED)
        Settings.Global.putString(
            MyApplication.instance.contentResolver,
            Settings.Global.ADB_ENABLED,
            null
        )

        assertEquals(DetectResult.Found(true), repository.detectAdb())
    }
}
