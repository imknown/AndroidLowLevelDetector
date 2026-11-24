package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties

// The domain contract of detectAdbAuthentication: the ro.adb.secure property read through the
// property reader. Values are staged via the Robolectric shadow the reflective read hits.
@RunWith(RobolectricTestRunner::class)
class DetectAdbAuthenticationTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `secure adb is found as enabled`() {
        ShadowSystemProperties.override(
            AndroidDataSource.PROP_ADB_SECURE,
            AndroidDataSource.SETTINGS_ENABLED.toString()
        )

        assertEquals(DetectResult.Found(true), repository.detectAdbAuthentication())
    }

    @Test
    fun `insecure adb is found as disabled`() {
        ShadowSystemProperties.override(
            AndroidDataSource.PROP_ADB_SECURE,
            AndroidDataSource.SETTINGS_DISABLED.toString()
        )

        assertEquals(DetectResult.Found(false), repository.detectAdbAuthentication())
    }

    @Test
    fun `absent property falls back to disabled`() {
        assertEquals(DetectResult.Found(false), repository.detectAdbAuthentication())
    }
}
