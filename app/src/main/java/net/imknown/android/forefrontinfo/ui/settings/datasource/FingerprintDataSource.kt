package net.imknown.android.forefrontinfo.ui.settings.datasource

import android.content.pm.PackageManager
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid13
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid9
import java.security.MessageDigest
import java.util.Locale

class FingerprintDataSource {
    companion object {
        private const val ALGORITHM_SHA256 = "SHA-256"

        private const val KEY_SHA_256_GOOGLE =
            "A7:0C:41:07:C1:FD:F0:3E:9A:F9:C4:6F:4B:38:18:1C:04:D0:F6:46:DA:E6:09:8C:22:45:3D:9E:D2:69:72:6C"

        private const val KEY_SHA_256_IMKNOWN =
            "67:28:46:79:62:50:DF:A8:BE:10:1E:46:59:97:F8:94:0C:4F:FC:BC:B6:62:EB:86:23:BF:62:A6:D0:70:39:85"

        private const val KEY_SHA_256_PUBLIC_DEBUG =
            "F1:42:FD:28:A5:AD:78:D5:A6:F4:41:3B:00:B5:16:29:74:91:05:8F:B2:3B:2A:37:15:31:E7:75:63:76:6D:18"
    }

    fun getPublicKeySha256OrNullOrThrow(packageManager: PackageManager, packageName: String): String? {
        val signatures = if (isAtLeastAndroid9()) {
            val packageInfo = if (isAtLeastAndroid13()) {
                val value = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                val flags = PackageManager.PackageInfoFlags.of(value)
                packageManager.getPackageInfo(packageName, flags)
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            }
            packageInfo?.signingInfo?.apkContentsSigners
        } else {
            @Suppress("Deprecation")
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("Deprecation")
            packageInfo?.signatures
        }

        return signatures?.getOrNull(0)?.let {
            MessageDigest.getInstance(ALGORITHM_SHA256).digest(it.toByteArray())
                .joinToString(":") { byte -> "%02x".format(byte) }
                .uppercase(Locale.US)
        }
    }

    fun getDistributor(mySha256: String?) = when (mySha256) {
        KEY_SHA_256_GOOGLE -> R.string.about_distributor_google
        KEY_SHA_256_IMKNOWN -> R.string.about_distributor_imknown
        KEY_SHA_256_PUBLIC_DEBUG -> R.string.about_distributor_public_debug
        else -> android.R.string.unknownName
    }
}