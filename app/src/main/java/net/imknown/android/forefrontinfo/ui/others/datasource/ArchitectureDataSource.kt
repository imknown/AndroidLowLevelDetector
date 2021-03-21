package net.imknown.android.forefrontinfo.ui.others.datasource

import android.annotation.SuppressLint
import android.os.Build
import net.imknown.android.forefrontinfo.binderdetector.BinderDetector
import net.imknown.android.forefrontinfo.ui.common.getStringProperty
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid6

class ArchitectureDataSource {
    companion object {
        // region [Binder]
        // const val CPU_ARCHITECTURE = "grep 'CPU architecture' /proc/cpuinfo"
        const val DRIVER_BINDER = "/dev/binder"

        const val ERRNO_NO_SUCH_FILE_OR_DIRECTORY = 2
        const val ERRNO_PERMISSION_DENIED = 13
        const val BINDER32_PROTOCOL_VERSION = 7
        const val BINDER64_PROTOCOL_VERSION = 8
        // endregion [Binder]

        private const val SYSTEM_PROPERTY_ARCHITECTURE = "os.arch"

        private const val PROP_RO_PRODUCT_CPU_ABI = "ro.product.cpu.abi"
    }

    // region [Binder]
    fun getBinderVersionOrThrow(driver: String): Int {
        System.loadLibrary(BinderDetector.PREFIX)
        return BinderDetector.getBinderVersion(driver)
    }
    // endregion [Binder]

    // region [Process]
    fun isProcess64BitOrThrow() = if (isAtLeastStableAndroid6()) {
        android.os.Process.is64Bit()
    } else {
        val vmRuntimePath = "dalvik.system.VMRuntime"

        @SuppressLint("DiscouragedPrivateApi")
        val vmRuntimeInstance = Class.forName(vmRuntimePath)
            .getDeclaredMethod("getRuntime")
            .invoke(null)

        @SuppressLint("DiscouragedPrivateApi")
        Class.forName(vmRuntimePath)
            .getDeclaredMethod("is64Bit")
            .invoke(vmRuntimeInstance) as Boolean
    }

    fun getArchitectureOrNullOrThrow(): String? = System.getProperty(SYSTEM_PROPERTY_ARCHITECTURE)
    // endregion [Process]

    // region [ABI]
    @Suppress("Deprecation")
    fun getCpuAbi(): String = Build.CPU_ABI
    fun getPropertyCpuAbi(): String = getStringProperty(PROP_RO_PRODUCT_CPU_ABI)
    fun getSupported32BitAbis(): Array<out String> = Build.SUPPORTED_32_BIT_ABIS
    fun getSupported64BitAbis(): Array<out String> = Build.SUPPORTED_64_BIT_ABIS
    // endregion [ABI]
}