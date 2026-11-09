package net.imknown.android.forefrontinfo.ui.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidVersionDataSource
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The domain contract of getOutdatedTargetSdkVersionApkState against the real package manager
// (spec 2026-10-25-22-16-01, subtask-04 batch 7): this entry scans the full installed package
// list, so per the means table it is instrumented-only; exact values are covered by each
// batch's golden snapshot comparison.
@RunWith(AndroidJUnit4::class)
class DetectOutdatedTargetSdkVersionApkTest {

    private val app by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as MyApplication
    }
    private val repository by lazy { app.appGraph.homeRepository }

    private fun detectStateOrderByPackageNameFirst(orderByPackageNameFirst: Boolean) =
        repository.getAssetLld(app.assets).let { lld ->
            assertNotNull("the bundled lld must load", lld)
            val result = repository.getOutdatedTargetSdkVersionApkState(lld, orderByPackageNameFirst)
            assertTrue("expected Found, got $result", result is DetectResult.Found)
            (result as DetectResult.Found).value
        }

    @Test
    fun packageNameFirstOrderSortsByPackageName() {
        val state = detectStateOrderByPackageNameFirst(true)

        val packageNames = state.outdatedApks.map { it.packageName }
        assertEquals(packageNames, packageNames.sorted())
        assertEquals(
            OutdatedTargetSdkVersionApkState.Order.PackageNameFirst,
            state.order
        )
    }

    @Test
    fun targetSdkVersionFirstOrderSortsBySdkThenPackageName() {
        val state = detectStateOrderByPackageNameFirst(false)

        // The same comparator shape the repository applies for this order.
        val sorted = state.outdatedApks.sortedWith(
            compareBy(
                OutdatedTargetSdkVersionApkState.OutdatedApk::targetSdkVersion,
                OutdatedTargetSdkVersionApkState.OutdatedApk::packageName
            )
        )
        assertEquals(sorted, state.outdatedApks)
        assertEquals(
            OutdatedTargetSdkVersionApkState.Order.TargetSdkVersionFirst,
            state.order
        )
    }

    @Test
    fun everyListedApkTargetsBelowTheCurrentApiLevel() {
        val state = detectStateOrderByPackageNameFirst(true)

        val currentApi = AndroidVersionDataSource.currentAndroidInfo.api
        assertTrue(
            "listed apks must target below $currentApi",
            state.outdatedApks.all { it.targetSdkVersion < currentApi }
        )

        // The match buckets are exhaustive by type; which one applies depends on the device's
        // system apps and the lld reference.
        when (state.match) {
            OutdatedTargetSdkVersionApkState.Match.Met,
            OutdatedTargetSdkVersionApkState.Match.Lagging,
            OutdatedTargetSdkVersionApkState.Match.Unknown,
            OutdatedTargetSdkVersionApkState.Match.None -> Unit
        }
    }
}
