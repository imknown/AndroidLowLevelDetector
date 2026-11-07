package net.imknown.android.forefrontinfo.ui.home.repository

import android.os.Build
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidVersionDataSource
import net.imknown.android.forefrontinfo.ui.home.model.AndroidInfoState
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectAndroid: a missing lld means the Unknown currency and the
// device's own version/dessert; a matching known row corrects the version and dessert; the
// five reference rows are carried in the lld's order; the currency compares the lld's stable
// and support api bars against the device. The Robolectric default environment runs a release
// codename (probed: SDK 37, CODENAME REL), so the stable branches are reachable and the
// preview branch is not; bars are built relative to Build.VERSION.SDK_INT to stay independent
// of that default.
@ExtendWith(RobolectricExtension::class)
class DetectAndroidTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `a missing lld means unknown currency and device facts`() {
        val result = repository.detectAndroid(null)

        assertTrue(result is DetectResult.Found)
        val state = (result as DetectResult.Found).value
        assertEquals(AndroidInfoState.AndroidCurrency.Unknown, state.currency)
        assertEquals(AndroidVersionDataSource.currentAndroidInfo.version, state.version)
        assertTrue(state.referenceRows.all {
            it.version == null && it.apiFull == null
        })
    }

    @Test
    fun `a matching known row corrects version and dessert`() {
        val androidInfo = AndroidVersionDataSource.currentAndroidInfo
        val lld = lldForTests(
            android = lldForTests().android.copy(
                known = listOf(
                    Lld.Androids.Android(
                        api = androidInfo.api.toString(),
                        apiFull = androidInfo.apiFull,
                        version = "16 corrected",
                        name = "Baklava corrected"
                    )
                )
            )
        )

        val state = (repository.detectAndroid(lld) as DetectResult.Found).value

        assertEquals("16 corrected", state.version)
        assertEquals("Baklava corrected", state.dessert)
    }

    @Test
    fun `reference rows carry the five upstream lines in lld order`() {
        val lld = lldForTests()

        val state = (repository.detectAndroid(lld) as DetectResult.Found).value

        assertEquals(
            listOf("36.1", "35.0", "37.0", "37.1", "37.2"),
            state.referenceRows.map { it.apiFull }
        )
    }

    @Test
    fun `the lld stable bar at the device api is latest`() {
        val base = lldForTests().android
        val lld = lldForTests(
            android = base.copy(stable = base.stable.copy(api = Build.VERSION.SDK_INT.toString()))
        )

        val state = (repository.detectAndroid(lld) as DetectResult.Found).value

        assertEquals(AndroidInfoState.AndroidCurrency.Latest, state.currency)
    }

    @Test
    fun `the stable bar above the device with the support bar reached is supported`() {
        val base = lldForTests().android
        val lld = lldForTests(
            android = base.copy(
                stable = base.stable.copy(api = (Build.VERSION.SDK_INT + 1).toString()),
                support = base.support.copy(api = Build.VERSION.SDK_INT.toString())
            )
        )

        val state = (repository.detectAndroid(lld) as DetectResult.Found).value

        assertEquals(AndroidInfoState.AndroidCurrency.Supported, state.currency)
    }

    @Test
    fun `the stable and support bars both above the device are outdated`() {
        val base = lldForTests().android
        val lld = lldForTests(
            android = base.copy(
                stable = base.stable.copy(api = (Build.VERSION.SDK_INT + 1).toString()),
                support = base.support.copy(api = (Build.VERSION.SDK_INT + 1).toString())
            )
        )

        val state = (repository.detectAndroid(lld) as DetectResult.Found).value

        assertEquals(AndroidInfoState.AndroidCurrency.Outdated, state.currency)
    }
}
