package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.PerformanceClassInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild

// The domain contract of detectPerformanceClass on a level that declares the class: the media
// performance class is staged through the Robolectric shadow.
@RunWith(RobolectricTestRunner::class)
class DetectPerformanceClassTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `a class matching the running level is met`() {
        ShadowBuild.setVersionMediaPerformanceClass(Build.VERSION.SDK_INT)

        assertEquals(
            DetectResult.Found(PerformanceClassInfo(Build.VERSION.SDK_INT, PerformanceClassInfo.Level.Met)),
            repository.detectPerformanceClass()
        )
    }

    @Test
    fun `one level behind is lagging`() {
        ShadowBuild.setVersionMediaPerformanceClass(Build.VERSION.SDK_INT - 1)

        assertEquals(
            DetectResult.Found(PerformanceClassInfo(Build.VERSION.SDK_INT - 1, PerformanceClassInfo.Level.Lagging)),
            repository.detectPerformanceClass()
        )
    }

    @Test
    fun `an older class is outdated`() {
        ShadowBuild.setVersionMediaPerformanceClass(Build.VERSION.SDK_INT - 5)

        assertEquals(
            DetectResult.Found(PerformanceClassInfo(Build.VERSION.SDK_INT - 5, PerformanceClassInfo.Level.Outdated)),
            repository.detectPerformanceClass()
        )
    }

    @Test
    fun `zero means not declared`() {
        ShadowBuild.setVersionMediaPerformanceClass(0)

        assertEquals(
            DetectResult.Found(PerformanceClassInfo(0, PerformanceClassInfo.Level.NotDeclared)),
            repository.detectPerformanceClass()
        )
    }
}
