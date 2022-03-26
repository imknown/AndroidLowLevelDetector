package net.imknown.android.forefrontinfo.ui.home

import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.suspendCancellableCoroutine
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.ui.base.JsonIo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GatewayApi {
    private const val HEADER_REFERER_KEY = "Referer"
    private const val HEADER_REFERER_VALUE = BuildConfig.APPLICATION_ID

    private const val REPOSITORY_NAME = "imknown/AndroidLowLevelDetector"

    private const val URL_PREFIX_LLD_JSON_GITEE = "gitee.com/$REPOSITORY_NAME/raw"
    private const val URL_PREFIX_LLD_JSON_GITHUB = "raw.githubusercontent.com/$REPOSITORY_NAME"

    suspend fun fetchLldJson() = suspendCancellableCoroutine<String> { cont ->
        val urlPrefixLldJson = if (isChinaMainlandTimezone()) {
            URL_PREFIX_LLD_JSON_GITEE
        } else {
            URL_PREFIX_LLD_JSON_GITHUB
        }

        "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/${JsonIo.LLD_JSON_NAME}"
            .httpGet()
            .header(HEADER_REFERER_KEY, HEADER_REFERER_VALUE)
            .responseString()
            .third
            .fold(
                { cont.resume(it) },
                { cont.resumeWithException(it) }
            )
    }
}