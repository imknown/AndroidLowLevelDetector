package net.imknown.android.forefrontinfo.ui.home.datasource

import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.headers
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone

// HttpClient is injected from the graph (the AppGraph httpClient binding) as a
// () -> HttpClient provider, and called once per request on purpose: the system
// proxy is read when the client is built, so a cached client would keep using
// the proxy it saw at build time. Zero footprint while the network toggle stays
// off (issues-cn #16 second half).
@Inject
class LldDataSource(
    private val httpClient: () -> HttpClient,
    private val lldFileStore: LldFileStore
) {
    companion object {
        // region [Online]
        private const val HEADER_REFERER_KEY = "Referer"
        private const val HEADER_REFERER_VALUE = BuildConfig.APPLICATION_ID

        private const val REPOSITORY_NAME = "imknown/AndroidLowLevelDetector"

        private const val URL_PREFIX_LLD_JSON_GITEE = "gitee.com/$REPOSITORY_NAME/raw"
        private const val URL_PREFIX_LLD_JSON_GITHUB = "raw.githubusercontent.com/$REPOSITORY_NAME"
        // endregion [Online]
    }

    suspend fun fetchOnlineLldJsonStringOrThrow(): String {
        val urlPrefixLldJson = if (isChinaMainlandTimezone()) {
            URL_PREFIX_LLD_JSON_GITEE
        } else {
            URL_PREFIX_LLD_JSON_GITHUB
        }

        val url = "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/${LldFileStore.LLD_JSON_NAME}"
        val response: HttpResponse = httpClient().get(url) {
            headers {
                append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE)
            }
        }
        return response.body()
    }

    fun fetchOfflineLldFileOrThrow() = lldFileStore.savedLldJsonFileOrThrow
}