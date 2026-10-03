package net.imknown.android.forefrontinfo.base

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.annotation.StringRes
import com.topjohnwu.superuser.Shell
import dev.zacsweers.metro.createGraphFactory
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.di.AppGraph
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
import java.io.File

// Theme mode: follow system / always light / always dark (the former 4th "power saver" option was dropped with the de-AppCompat change)
// An enum rather than a resource string: the default value needs no SharedPreferences/resources, so it can be non-null
enum class AppThemeMode {
    FollowSystem,
    AlwaysLight,
    AlwaysDark;

    // The single three-way "is the app dark" mapping: always light = false, always dark = true,
    // follow system defers to the caller-provided system value. Shared by MainActivity (window shell +
    // edge-to-edge) and AppTheme (colors), so the two can never drift apart.
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        FollowSystem -> systemDark
        AlwaysLight -> false
        AlwaysDark -> true
    }
}

// Scroll bar mode: none / normal / draggable (the third one is the View-era fast scroll, whose option
// key is commented out in arrays.xml and whose RecyclerView FastScroller was dropped as buggy in 2019)
// The mode is kept as three cases rather than collapsed to a flag: a stored "2" stays itself, so the
// setting row and the UI can disagree about what is on screen without one of them losing the value
enum class ScrollBarMode {
    None,
    Normal,
    Draggable;

    // What the pages ask: is a scroll indicator drawn. Only Normal is, today -- Draggable has no
    // implementation yet, which is the same outcome the legacy setScrollBarMode reached (its when()
    // matched only the normal value)
    val drawsScrollBar: Boolean
        get() = this == Normal
}

open class MyApplication : Application() {

    // The single DI graph instance (composition root, issues-cn #02). Metro
    // resolves the whole object chain at compile time; this only instantiates
    // the generated graph class through its factory, handing it `this` as the
    // Application binding (the entry point the Context-derived bindings build
    // on, e.g. ST-10's SharedPreferences). Deliberately eager rather than lazy:
    // MainActivity.setContent reads appGraph.metroViewModelFactory, and
    // Application is always constructed before any of its Activities.
    val appGraph: AppGraph = createGraphFactory<AppGraph.Factory>().create(this)

    fun getDownloadDir() = getFileDir(Environment.DIRECTORY_DOWNLOADS)

    private fun getFileDir(type: String): File {
        val externalFilesDir = getExternalFilesDir(type)
        return if (externalFilesDir != null && externalFilesDir.exists()) {
            externalFilesDir
        } else {
            filesDir.resolve(type)
        }.apply {
            mkdirs()
        }
    }

    companion object {
        lateinit var instance: MyApplication

        val sharedPreferences: SharedPreferences by lazy {
            instance.getSharedPreferences("${instance.packageName}_preferences", Context.MODE_PRIVATE)
        }

        fun getMyString(@StringRes resId: Int) =
            instance.getString(resId)

        fun getMyString(@StringRes resId: Int, vararg formatArgs: Any?) =
            instance.getString(resId, *formatArgs)
    }

    override fun onCreate() {
        super.onCreate()

        instance = this@MyApplication

        initMyAndroid()

        // First resolution of the settings store = construction = synchronous seeding of both
        // mode flows from SP, pinned here where initTheme sat: instance and SP exist from this
        // point, so MainActivity's pre-composition reads and every collect downstream see the
        // persisted values. The access itself is the point (the seed); both mode flows'
        // consumers have switched to the store (ST-02/03), the boolean switches' consumers
        // switch in ST-04. The unused-result warning is inherent to a
        // resolve-for-side-effect statement and is suppressed deliberately.
        @Suppress("RETURN_VALUE_NOT_USED")
        appGraph.settingsStore

        initShell()
    }

    private fun initShell() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.enableLegacyStderrRedirection = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_NON_ROOT_SHELL)
//                .setInitializers(Shell.Initializer::class.java)
        )
    }
}