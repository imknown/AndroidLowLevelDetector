package net.imknown.android.forefrontinfo.di

import android.net.TrafficStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
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
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.property.IProperty
import net.imknown.android.forefrontinfo.base.property.impl.PropertyDefault
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
// @Provides at all. Leaf bindings whose types cannot carry an @Inject
// constructor live in the binding containers below (attached via
// @DependencyGraph's bindingContainers parameter): the HttpClient singleton is
// the first, the IProperty binding (PropertyDefault lives in :base) the second;
// SharedPreferences and the lld file store follow in the later subtasks.
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
@DependencyGraph(
    AppScope::class,
    bindingContainers = [HttpClientContainer::class, PropertyContainer::class]
)
interface AppGraph : ViewModelGraph {

    // The graph's second external entry point: MyApplication cannot be graph-
    // constructed (the graph itself is created in its constructor), so the
    // factory binds `this` in via a @Provides parameter. This is the foundation
    // for the Context-derived bindings to come (ST-10's SharedPreferences);
    // until now the graph was all @Inject constructor chains and needed no
    // entry point.
    //
    // No companion object in source: on Metro 1.4.5, a graph with BOTH an
    // existing companion and a nested factory never gets its SAM implementation
    // generated (reproduced with fun create, operator fun invoke, and an
    // explicit source-declared companion object : Factory). Companion-free
    // graphs get a generated companion implementing the factory instead --
    // hence the @Provides moving from the companion into the binding
    // containers. Worth rechecking after a Metro upgrade.
    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides app: MyApplication): AppGraph
    }
}

@BindingContainer
object HttpClientContainer {
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

@BindingContainer
object PropertyContainer {
    // The IProperty binding: PropertyDefault lives in :base, keeping the
    // interface module free of Metro annotations. Reflective SystemProperties
    // reads are stateless -- the provider returns the same object every time,
    // so no @SingleIn is needed.
    @Provides
    fun property(): IProperty = PropertyDefault
}
