package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.DynamicPartitionsState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties

// The domain contract of detectDynamicPartitions: either flag turns it on; retrofit is reported
// separately because the mapper appends its own text for it.
@RunWith(RobolectricTestRunner::class)
class DetectDynamicPartitionsTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `both flags absent means plain disabled`() {
        assertEquals(
            DetectResult.Found(DynamicPartitionsState(enabled = false, retrofit = false)),
            repository.detectDynamicPartitions()
        )
    }

    @Test
    fun `main flag alone means enabled without retrofit`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_DYNAMIC_PARTITIONS, "1")

        assertEquals(
            DetectResult.Found(DynamicPartitionsState(enabled = true, retrofit = false)),
            repository.detectDynamicPartitions()
        )
    }

    @Test
    fun `retrofit flag alone means enabled and retrofitted`() {
        ShadowSystemProperties.override(AndroidDataSource.PROP_DYNAMIC_PARTITIONS_RETROFIT, "1")

        assertEquals(
            DetectResult.Found(DynamicPartitionsState(enabled = true, retrofit = true)),
            repository.detectDynamicPartitions()
        )
    }
}
