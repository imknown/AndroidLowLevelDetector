package net.imknown.android.forefrontinfo.base

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Environment
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.property.impl.DefaultProperty
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.base.shell.impl.LibSuShell
import net.imknown.android.forefrontinfo.ui.base.Event
import java.io.File

open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

        private val _sharedFlow = MutableSharedFlow<Boolean>()
        val sharedFlow = _sharedFlow.asSharedFlow()

        private val _homeLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }
        val homeLanguageEvent: LiveData<Event<Unit>> by lazy { _homeLanguageEvent }
        private val _othersLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }
        val othersLanguageEvent: LiveData<Event<Unit>> by lazy { _othersLanguageEvent }
        private val _propLanguageEvent by lazy { MutableLiveData<Event<Unit>>() }
        val propLanguageEvent: LiveData<Event<Unit>> by lazy { _propLanguageEvent }

        val sharedPreferences: SharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(instance)
        }

        fun getDownloadDir() = getFileDir(Environment.DIRECTORY_DOWNLOADS)

        private fun getFileDir(type: String): File {
            val externalFilesDir = instance.getExternalFilesDir(type)
            return if (externalFilesDir != null && externalFilesDir.exists()) {
                externalFilesDir
            } else {
                instance.filesDir.resolve(type)
            }.apply {
                mkdirs()
            }
        }

        fun getMyString(@StringRes resId: Int) =
            instance.getString(resId)

        fun getMyString(@StringRes resId: Int, vararg formatArgs: Any?) =
            instance.getString(resId, *formatArgs)

        fun setMyTheme(themesValue: String) {
            @AppCompatDelegate.NightMode val mode = when (themesValue) {
                getMyString(R.string.interface_themes_follow_system_value) -> {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                getMyString(R.string.interface_themes_power_saver_value) -> {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
                getMyString(R.string.interface_themes_always_light_value) -> {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                getMyString(R.string.interface_themes_always_dark_value) -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
                else -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
            }

            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this@MyApplication

        initTheme()

        initShellAndProperty()

        initLanguage()
    }

    private fun initTheme() {
        DynamicColors.applyToActivitiesIfAvailable(this)

        val themesValue = sharedPreferences.getString(
            getMyString(R.string.interface_themes_key), null
        ) ?: getMyString(R.string.interface_themes_follow_system_value)
        setMyTheme(themesValue)
    }

    private fun initShellAndProperty() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.enableLegacyStderrRedirection = true
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_NON_ROOT_SHELL)
//                .setInitializers(Shell.Initializer::class.java)
        )

        ShellManager.instance = ShellManager(LibSuShell)

        PropertyManager.instance = PropertyManager(DefaultProperty)
    }

    private fun initLanguage() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                _homeLanguageEvent.value = Event(Unit)
                _othersLanguageEvent.value = Event(Unit)
                _propLanguageEvent.value = Event(Unit)
            }
        }

        registerReceiver(receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }
}