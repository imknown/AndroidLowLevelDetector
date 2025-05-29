package net.imknown.android.forefrontinfo.ui.home

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.headers
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.ui.base.JsonIo
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

object GatewayApi {
    private const val HEADER_REFERER_KEY = "Referer"
    private const val HEADER_REFERER_VALUE = BuildConfig.APPLICATION_ID

    private const val REPOSITORY_NAME = "imknown/AndroidLowLevelDetector"

    private const val URL_PREFIX_LLD_JSON_GITEE = "gitee.com/$REPOSITORY_NAME/raw"
    private const val URL_PREFIX_LLD_JSON_GITHUB = "raw.githubusercontent.com/$REPOSITORY_NAME"

    suspend fun fetchLldJson(): String {
        val urlPrefixLldJson = if (isChinaMainlandTimezone()) {
            URL_PREFIX_LLD_JSON_GITEE
        } else {
            URL_PREFIX_LLD_JSON_GITHUB
        }

        val client = HttpClient(OkHttp) {
            engine {
                config {
                    // region [Proxy]
                    // Fix: java.lang.IllegalArgumentException: port out of range:-1
                    // Steps to reproduce (small probability): Change Wifi proxy from "Manual" to "PAC"
                    // https://github.com/square/okhttp/issues/6877#issuecomment-1438554879
                    val proxySelector = object : ProxySelector() {
                        override fun select(uri: URI?): List<Proxy> = try {
                            getDefault().select(uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            listOf(Proxy.NO_PROXY)
                        }

                        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                            getDefault().connectFailed(uri, sa, ioe)
                        }
                    }
                    proxySelector(proxySelector)
                    // endregion [Proxy]
                }
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            }
        }

        val url = "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/${JsonIo.LLD_JSON_NAME}"
        val response: HttpResponse = client.get(url) {
            headers {
                append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE)
            }
        }
        return client.use {
            response.body()
        }
    }
}