package net.imknown.android.forefrontinfo

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.ProgressCallback
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import java.io.File
import java.util.*

class GatewayApi {
    companion object {
        internal const val LLD_JSON_NAME = "lld.json"

        private const val URL_LLD_JSON_ZH_CN =
            "https://gitee.com/imknown/AndroidLowLevelDetector/raw/master/app/src/main/assets/$LLD_JSON_NAME"

        private const val URL_LLD_JSON =
            "https://raw.githubusercontent.com/imknown/AndroidLowLevelDetector/master/app/src/main/assets/$LLD_JSON_NAME"

        private const val URL_GITHUB_CHECK_FOR_UPDATE =
            "https://api.github.com/repos/imknown/AndroidLowLevelDetector/releases/latest"

        internal const val DIR_APK = "Apk"

        lateinit var savedLldJsonFile: File

        internal suspend fun downloadLldJsonFile(): Boolean {
            val url = if (isZhCn()) {
                URL_LLD_JSON_ZH_CN
            } else {
                URL_LLD_JSON
            }

            val (_, response, result) = url.httpDownload()
                .fileDestination { _, _ -> savedLldJsonFile }
                .awaitByteArrayResponseResult()
            val (byteArray, error) = result

            if (BuildConfig.DEBUG && error != null) {
                error.printStackTrace()
            }

            return response.isSuccessful && byteArray != null && error == null
        }

        internal suspend fun checkForUpdate(
            success: (String) -> Unit,
            failure: (FuelError) -> Unit
        ) {
            val url = URL_GITHUB_CHECK_FOR_UPDATE
            url.httpGet().awaitStringResult().fold(success, failure)
        }

        internal suspend fun downloadApk(
            url: String,
            fileName: String,
            progressCallback: ProgressCallback,
            success: (ByteArray) -> Unit,
            failure: (FuelError) -> Unit
        ) {
            val (_, _, result) = url.httpDownload()
                .fileDestination { _, _ ->
                    MyApplication.getApkDir().deleteRecursively()
                    // emptyDir(MyApplication.getApkDir())
                    MyApplication.getApkDir().mkdirs()
                    File(MyApplication.getApkDir(), fileName)
                }
                .responseProgress(progressCallback)
                .awaitByteArrayResponseResult()

            result.fold(success, failure)
        }

        private fun emptyDir(dir: File, deleteItself: Boolean) {
            dir.walkTopDown().forEach { it.delete() }
            dir.takeIf { deleteItself }?.delete()
        }

        private fun isZhCn(): Boolean {
            val lD = Locale.getDefault()
            val lSC = Locale.SIMPLIFIED_CHINESE
            return (lD.language == lSC.language && lD.country == lSC.country)
        }
    }
}