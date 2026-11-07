package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.model.KernelState
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectKernel: the os.version read goes through the JVM system
// properties (staged with System.setProperty: the Android property shadow is a different
// store and would never reach this read). The device reports a bare version string, no
// "Linux version" prefix: the version library does not scan past a textual prefix. The match
// judgment follows the lld's version order (last matching major+minor wins, as before).
// An absent os.version fails the detection (batch 4 equivalence exception, recorded in the
// spec progress); a non-null value is always carried, however oddly it parses.
@ExtendWith(RobolectricExtension::class)
class DetectKernelTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    private fun lldWithKernelVersions(versions: List<String>, mainline: String = "6.12"): Lld {
        val base = lldForTests()
        return base.copy(linux = base.linux.copy(google = base.linux.google.copy(versions = versions)))
    }

    private fun stageKernel(value: String) {
        System.setProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION, value)
    }

    @Test
    fun `a kernel at the supported level is met`() {
        stageKernel("5.10.101-android13")
        val lld = lldWithKernelVersions(listOf("5.10", "5.4"))

        assertEquals(
            DetectResult.Found(
                KernelState(
                    linuxVersionString = "5.10.101-android13",
                    supportedVersions = listOf("5.10", "5.4"),
                    mainlineVersion = "6.12",
                    match = KernelState.KernelMatch.Met
                )
            ),
            repository.detectKernel(lld)
        )
    }

    @Test
    fun `a kernel patch behind a three-part bar is lagging`() {
        stageKernel("5.10.50-android13")
        val lld = lldWithKernelVersions(listOf("5.10.99", "5.4"))

        assertEquals(
            DetectResult.Found(
                KernelState(
                    linuxVersionString = "5.10.50-android13",
                    supportedVersions = listOf("5.10.99", "5.4"),
                    mainlineVersion = "6.12",
                    match = KernelState.KernelMatch.Lagging
                )
            ),
            repository.detectKernel(lld)
        )
    }

    @Test
    fun `no matching major-minor means none`() {
        stageKernel("4.19.300-android11")
        val lld = lldWithKernelVersions(listOf("5.10", "5.4"))

        assertEquals(
            DetectResult.Found(
                KernelState(
                    linuxVersionString = "4.19.300-android11",
                    supportedVersions = listOf("5.10", "5.4"),
                    mainlineVersion = "6.12",
                    match = KernelState.KernelMatch.None
                )
            ),
            repository.detectKernel(lld)
        )
    }

    @Test
    fun `a missing lld list still carries the raw version`() {
        stageKernel("5.10.101-android13")

        assertEquals(
            DetectResult.Found(
                KernelState(
                    linuxVersionString = "5.10.101-android13",
                    supportedVersions = null,
                    mainlineVersion = null,
                    match = KernelState.KernelMatch.None
                )
            ),
            repository.detectKernel(null)
        )
    }

    @Test
    fun `the last matching lld entry wins`() {
        stageKernel("5.10.50-android13")
        val lld = lldWithKernelVersions(listOf("5.10", "5.10.99"))

        assertEquals(
            DetectResult.Found(
                KernelState(
                    linuxVersionString = "5.10.50-android13",
                    supportedVersions = listOf("5.10", "5.10.99"),
                    mainlineVersion = "6.12",
                    match = KernelState.KernelMatch.Lagging
                )
            ),
            repository.detectKernel(lld)
        )
    }

    @Test
    fun `an absent os version fails the detection`() {
        val original = System.getProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION)
        try {
            System.clearProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION)

            assertTrue(repository.detectKernel(lldForTests()) is DetectResult.Failed)
        } finally {
            // os.version is a JVM-global property; the cleared state must not leak past this test.
            if (original != null) {
                System.setProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION, original)
            }
        }
    }
}
