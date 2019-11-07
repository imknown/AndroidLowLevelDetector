package net.imknown.android.forefrontinfo

import com.github.kittinunf.fuel.core.ResponseResultHandler
import com.github.kittinunf.fuel.httpDownload
import java.io.File
import java.util.*

class GatewayApi {
    companion object {
        internal const val LLD_JSON_NAME = "lld.json"

        private const val URL_LLD_JSON_CN_ZH =
            "https://coding.net/u/imknown/p/1/git/raw/master/$LLD_JSON_NAME"

        private const val URL_LLD_JSON =
            "https://raw.githubusercontent.com/imknown/AndroidLowLevelDetector/master/app/src/main/assets/$LLD_JSON_NAME"

        lateinit var savedFile: File

        fun downloadLldJsonFile(handler: ResponseResultHandler<ByteArray>) {
            val lD = Locale.getDefault()
            val lSC = Locale.SIMPLIFIED_CHINESE
            val url = if (lD.language == lSC.language && lD.country == lSC.country) {
                URL_LLD_JSON_CN_ZH
            } else {
                URL_LLD_JSON
            }

            // /* Gson */ url.httpGet().responseObject<Lld>(handler)
            url.httpDownload()
                .fileDestination { _, _ -> savedFile }
                .response(handler)
        }
    }
}