package net.imknown.android.forefrontinfo.base

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.property.PropertyManager
import net.imknown.android.forefrontinfo.base.property.impl.PropertyDefault
import net.imknown.android.forefrontinfo.base.shell.ShellManager
import net.imknown.android.forefrontinfo.ui.common.ShellLibSu
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
import java.io.File

// Theme mode: follow system / always light / always dark (the former 4th "power saver" option was dropped with the de-AppCompat change)
// An enum rather than a resource string: the default value needs no SharedPreferences/resources, so it can be non-null
enum class AppThemeMode {
    FollowSystem,
    AlwaysLight,
    AlwaysDark
}

open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

        val sharedPreferences: SharedPreferences by lazy {
            instance.getSharedPreferences("${instance.packageName}_preferences", Context.MODE_PRIVATE)
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

        // Single source of truth for the theme mode: non-null, defaults to follow system;
        // overwritten from the persisted value by initTheme on startup, and written via setMyTheme when Settings changes it—
        // AppTheme collects it, so subscribers recompose automatically (replacing the old AppCompatDelegate Activity recreate).
        // Uses the project's explicit backing-field convention: exposes StateFlow, writable as .value inside the class
        val themeMode: StateFlow<AppThemeMode>
            field = MutableStateFlow<AppThemeMode>(AppThemeMode.FollowSystem)

        fun setMyTheme(themesValue: String?) {
            // Translate the persisted mode string into the enum (the single write entry); unrecognized values (including the legacy "power saver" and null) fall back to follow system
            val mode = when (themesValue) {
                getMyString(R.string.interface_themes_always_light_value) -> AppThemeMode.AlwaysLight
                getMyString(R.string.interface_themes_always_dark_value) -> AppThemeMode.AlwaysDark
                else -> AppThemeMode.FollowSystem
            }
            themeMode.value = mode
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this@MyApplication

        initMyAndroid()

        initTheme()

        initShellAndProperty()
    }

    private fun initTheme() {
        // DynamicColors.applyToActivitiesIfAvailable (View-side dynamic color) is retired along with the View system:
        // dynamic color is now owned solely by Compose's AppTheme(dynamicColor = true), avoiding two sources of truth.

        // On startup, seed the mode stored in SharedPreferences into the stream (getString supplies the default when absent, so no trailing ?: fallback is needed)
        val themeKey = getMyString(R.string.interface_themes_key)
        val defaultTheme = getMyString(R.string.interface_themes_follow_system_value)
        val themesValue = sharedPreferences.getString(themeKey, defaultTheme)

        // One-time migration for the retired "power saver" mode (its tombstone value "1" is kept in
        // strings.xml so the number is never recycled): normalize the stored preference back to follow
        // system AND write it through, so every later read (e.g. SettingsScreen) sees a clean value
        // instead of relying on setMyTheme's fallback on every launch.
        if (themesValue == getMyString(R.string.interface_themes_power_saver_value)) {
            sharedPreferences.edit { putString(themeKey, defaultTheme) }
        }

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

        ShellManager.instance = ShellManager(ShellLibSu)

        PropertyManager.instance = PropertyManager(PropertyDefault)
    }
}