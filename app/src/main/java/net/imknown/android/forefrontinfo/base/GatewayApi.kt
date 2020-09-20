package net.imknown.android.forefrontinfo.base

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import net.imknown.android.forefrontinfo.BuildConfig

object GatewayApi {
    private const val REPOSITORY_NAME = "imknown/AndroidLowLevelDetector"

    private const val URL_PREFIX_LLD_JSON_ZH_CN = "gitee.com/$REPOSITORY_NAME/raw"
    private const val URL_PREFIX_LLD_JSON = "raw.githubusercontent.com/$REPOSITORY_NAME"

    suspend fun fetchLldJson(
        success: (String) -> Unit,
        failure: (FuelError) -> Unit
    ) {
        val urlPrefixLldJson = if (isChinaMainlandTimezone()) {
            URL_PREFIX_LLD_JSON_ZH_CN
        } else {
            URL_PREFIX_LLD_JSON
        }

        val url =
            "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/${JsonIo.LLD_JSON_NAME}"
        url.httpGet().awaitStringResult().fold(success, failure)
    }
}