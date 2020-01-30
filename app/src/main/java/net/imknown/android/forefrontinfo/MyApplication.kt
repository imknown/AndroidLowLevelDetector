package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.os.Environment
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.base.Event
import java.io.File

@SuppressLint("Registered")
open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

        val homeLanguageEvent: LiveData<Event<Int>> by lazy { instance._homeLanguageEvent }
        val settingsLanguageEvent: LiveData<Event<Int>> by lazy { instance._settingsLanguageEvent }

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
            }
        }

        fun getMyString(@StringRes resId: Int) =
            instance.getString(resId)

        fun getMyString(@StringRes resId: Int, vararg formatArgs: Any?) =
            instance.getString(resId, *formatArgs)
    }

    private val _homeLanguageEvent by lazy { MutableLiveData<Event<Int>>() }
    private val _settingsLanguageEvent by lazy { MutableLiveData<Event<Int>>() }

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch(Dispatchers.IO) {
            instance = this@MyApplication

            initShell()

            initTheme()

            initLanguage()
        }
    }

    private fun initShell() {
        // Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
        Shell.Config.setFlags(Shell.FLAG_NON_ROOT_SHELL)
        Shell.Config.verboseLogging(BuildConfig.DEBUG)
        // Shell.Config.setTimeout(10)
    }

    private suspend fun initTheme() {
        val themesValue = sharedPreferences.getString(
            getMyString(R.string.interface_themes_key),
            getMyString(R.string.interface_themes_follow_system_value)
        )!!
        setMyTheme(themesValue)
    }

    suspend fun setMyTheme(themesValue: String) = withContext(Dispatchers.Default) {
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

        withContext(Dispatchers.Main) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private suspend fun initLanguage() = withContext(Dispatchers.Main) {
        LanguageBroadcastLiveData().observeForever {
            _homeLanguageEvent.value = Event(0)
            _settingsLanguageEvent.value = Event(0)
        }
    }
}