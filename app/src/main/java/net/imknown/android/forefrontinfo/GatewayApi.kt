package net.imknown.android.forefrontinfo

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import com.github.kittinunf.fuel.httpDownload
import java.io.File
import java.util.*

class GatewayApi {
    companion object {
        const val LLD_JSON_NAME = "lld.json"

        private const val REPOSITORY_NAME = "imknown/AndroidLowLevelDetector"

        private const val URL_PREFIX_LLD_JSON_ZH_CN = "gitee.com/$REPOSITORY_NAME/raw"
        private const val URL_PREFIX_LLD_JSON = "raw.githubusercontent.com/$REPOSITORY_NAME"

        val savedLldJsonFile: File by lazy {
            File(MyApplication.getDownloadDir(), LLD_JSON_NAME)
        }

        suspend fun downloadLldJsonFile(
            success: (ByteArray) -> Unit,
            failure: (FuelError) -> Unit
        ) {
            val urlPrefixLldJson = if (isChinaMainlandTimezone()) {
                URL_PREFIX_LLD_JSON_ZH_CN
            } else {
                URL_PREFIX_LLD_JSON
            }

            val url =
                "https://$urlPrefixLldJson/${BuildConfig.FLAVOR}/app/src/main/assets/$LLD_JSON_NAME"

            url.httpDownload().fileDestination { _, _ -> savedLldJsonFile }
                .awaitByteArrayResult().fold(success, failure)
        }

        private fun isChinaMainlandTimezone() =
            TimeZone.getDefault().id == "Asia/Shanghai" || TimeZone.getDefault().id == "Asia/Urumqi"
    }
}