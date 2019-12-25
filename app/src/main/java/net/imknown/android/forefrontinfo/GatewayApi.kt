package net.imknown.android.forefrontinfo

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import com.github.kittinunf.fuel.httpDownload
import java.io.File
import java.util.*

class GatewayApi {
    companion object {
        const val LLD_JSON_NAME = "lld.json"

        private const val URL_LLD_JSON_ZH_CN =
            "https://gitee.com/imknown/AndroidLowLevelDetector/raw/master/app/src/main/assets/$LLD_JSON_NAME"

        private const val URL_LLD_JSON =
            "https://raw.githubusercontent.com/imknown/AndroidLowLevelDetector/master/app/src/main/assets/$LLD_JSON_NAME"

        val savedLldJsonFile: File by lazy {
            File(MyApplication.getDownloadDir(), LLD_JSON_NAME)
        }

        suspend fun downloadLldJsonFile(
            success: (ByteArray) -> Unit,
            failure: (FuelError) -> Unit
        ) {
            val url = if (isChinaMainlandTimezone()) {
                URL_LLD_JSON_ZH_CN
            } else {
                URL_LLD_JSON
            }

            url.httpDownload().fileDestination { _, _ -> savedLldJsonFile }
                .awaitByteArrayResult().fold(success, failure)
        }

        private fun isChinaMainlandTimezone() =
            TimeZone.getDefault().id == "Asia/Shanghai" || TimeZone.getDefault().id == "Asia/Urumqi"
    }
}