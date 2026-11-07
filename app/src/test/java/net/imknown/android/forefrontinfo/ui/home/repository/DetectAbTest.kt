package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.AbState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.shadows.ShadowSystemProperties
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectAb: three independent switches (the master flag, the slot suffix,
// virtual A/B) OR into enabled; the retrofit variant is only read when virtual A/B is on. An
// absent property surfaces as null (the absence-aware property read), not as display text.
@ExtendWith(RobolectricExtension::class)
class DetectAbTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `all sources absent means disabled without virtual ab`() {
        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = false,
                    virtualAb = false,
                    virtualAbRetrofit = false,
                    slotSuffix = null
                )
            ),
            repository.detectAb()
        )
    }

    @Test
    fun `master flag alone enables plain ab`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_AB_UPDATE, "1")

        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = true,
                    virtualAb = false,
                    virtualAbRetrofit = false,
                    slotSuffix = null
                )
            ),
            repository.detectAb()
        )
    }

    @Test
    fun `slot suffix alone enables ab`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_SLOT_SUFFIX, "_a")

        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = true,
                    virtualAb = false,
                    virtualAbRetrofit = false,
                    slotSuffix = "_a"
                )
            ),
            repository.detectAb()
        )
    }

    @Test
    fun `virtual ab flag enables and carries the variant`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VIRTUAL_AB_ENABLED, "1")

        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = true,
                    virtualAb = true,
                    virtualAbRetrofit = false,
                    slotSuffix = null
                )
            ),
            repository.detectAb()
        )
    }

    @Test
    fun `virtual ab retrofit is read only when virtual ab is on`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VIRTUAL_AB_ENABLED, "1")
        ShadowSystemProperties.override(AndroidDataSource.PROP_VIRTUAL_AB_RETROFIT, "1")

        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = true,
                    virtualAb = true,
                    virtualAbRetrofit = true,
                    slotSuffix = null
                )
            ),
            repository.detectAb()
        )
    }

    @Test
    fun `retrofit flag without virtual ab stays off`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_VIRTUAL_AB_RETROFIT, "1")

        assertEquals(
            DetectResult.Found(
                AbState(
                    enabled = false,
                    virtualAb = false,
                    virtualAbRetrofit = false,
                    slotSuffix = null
                )
            ),
            repository.detectAb()
        )
    }
}
