package net.imknown.android.forefrontinfo.di

import android.net.TrafficStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid16
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Protocol
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

// Composition root: the only place in the whole project that knows "who needs
// whom" (issues-cn #02/#05). Metro resolves the entire graph at compile time
// from @Inject constructors, and a constructor-injected class is bound to its
// concrete type automatically, so the all-concrete ViewModel chains need no
// @Provides at all. Providers here are for leaf bindings whose types cannot
// carry an @Inject constructor — the HttpClient singleton is the first;
// SharedPreferences and the remaining static singletons follow in the later
// subtasks.
//
// Extending ViewModelGraph brings in MetroX's three ViewModel multibindings
// plus the metroViewModelFactory accessor that MainActivity provides down to
// Compose. That accessor can only resolve if a MetroViewModelFactory binding
// exists — supplied by AppViewModelFactory (via @ContributesBinding); without
// it the graph fails to compile with [Metro/MissingBinding].
//
// AppScope is Metro's application-lifetime scope: a @SingleIn(AppScope) binding
// caches one instance per graph, and this graph is the only one in the process.
// ViewModels deliberately get no @SingleIn — their lifetime belongs to the
// ViewModelStore (one per Navigation 3 entry), so the factory must emit a
// fresh instance on every create call.
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {

    companion object {
        // The project's single HttpClient binding (issues-cn #16 second half):
        // the OkHttp engine config moved verbatim from LldDataSource
        // (TrafficStats tagging / Wi-Fi PAC proxy fallback / debug Logging),
        // logic untouched. @SingleIn caches the client with the graph; resolved
        // lazily through Provider injection in LldDataSource, so it is built on
        // the first real request and the pool is reused across refreshes.
        // Behavior change from the old per-request client: client.use {}'s
        // close-after-use is gone -- Metro does no close, the process lifetime
        // is the client lifetime, a net win for this single-request use case.
        @Provides
        @SingleIn(AppScope::class)
        fun httpClient(): HttpClient = HttpClient(OkHttp) {
            engine {
                config {
                    // https://github.com/square/okhttp/issues/3537#issuecomment-3391015783
                    val eventListener = object : EventListener() {
                        override fun connectStart(
                            call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy
                        ) {
                            val thread = Thread.currentThread()
                            val id = if (isAtLeastAndroid16()) {
                                thread.threadId()
                            } else {
                                @Suppress("DEPRECATION")
                                thread.id
                            }.toInt()
                            TrafficStats.setThreadStatsTag(id)
                        }

                        override fun connectEnd(
                            call: Call,
                            inetSocketAddress: InetSocketAddress,
                            proxy: Proxy,
                            protocol: Protocol?
                        ) {
                            TrafficStats.clearThreadStatsTag()
                        }
                    }
                    eventListener(eventListener)

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
                            ioe?.printStackTrace()
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
    }
}
