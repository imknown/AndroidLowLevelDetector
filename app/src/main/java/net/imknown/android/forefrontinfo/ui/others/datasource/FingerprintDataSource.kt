package net.imknown.android.forefrontinfo.ui.others.datasource

import android.os.Build
import net.imknown.android.forefrontinfo.ui.common.getStringProperty

class FingerprintDataSource {
    companion object {
        private const val PROP_PREVIEW_SDK_FINGERPRINT = "ro.build.version.preview_sdk_fingerprint" // Build.VERSION.PREVIEW_SDK_FINGERPRINT
    }

    fun getFingerprint(): String = Build.FINGERPRINT

    fun getPreviewSdkFingerprint() = getStringProperty(PROP_PREVIEW_SDK_FINGERPRINT)

    /** [Build.getFingerprintedPartitions] */
    fun getPartitions() = arrayOf(
        "bootimage", "odm", "odm_dlkm", "oem", "product", "system", "system_dlkm", "system_ext", "vendor", "vendor_dlkm"
    )

    fun getPartitionFingerprint(name: String) = "ro.$name.build.fingerprint"

    fun getPartitionFingerprintProperty(partitionFingerprintProperty: String) =
        getStringProperty(partitionFingerprintProperty)
}