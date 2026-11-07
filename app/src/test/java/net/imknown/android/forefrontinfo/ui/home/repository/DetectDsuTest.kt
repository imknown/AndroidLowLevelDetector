package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.shadows.ShadowSystemProperties
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectDsu: either of the two flags turns it on; both absent means off
// (the property reader's default for booleans).
@ExtendWith(RobolectricExtension::class)
class DetectDsuTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `both flags absent means disabled`() {
        assertEquals(DetectResult.Found(false), repository.detectDsu())
    }

    @Test
    fun `persist flag alone turns it on`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE, "1")

        assertEquals(DetectResult.Found(true), repository.detectDsu())
    }

    @Test
    fun `non-persist flag alone turns it on`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_DYNAMIC_SYSTEM_UPDATE, "1")

        assertEquals(DetectResult.Found(true), repository.detectDsu())
    }
}
