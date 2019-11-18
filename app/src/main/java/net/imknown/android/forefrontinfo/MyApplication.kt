package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("Registered")
open class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication

        internal fun getApkDir() = getDownloadDir().resolve(GatewayApi.DIR_APK)

        internal fun getDownloadDir() = getFileDir(Environment.DIRECTORY_DOWNLOADS)

        private fun getFileDir(type: String): File {
            val externalFilesDir = instance.getExternalFilesDir(type)
            return if (externalFilesDir != null && externalFilesDir.exists()) {
                externalFilesDir
            } else {
                instance.filesDir.resolve(type)
            }
        }

        internal suspend fun setMyTheme(themesValue: String) {
            @AppCompatDelegate.NightMode val mode =
                AppCompatDelegate::class.java.getDeclaredField(themesValue).getInt(null)

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        fun getMyString(@StringRes resId: Int) =
            instance.getString(resId)

        fun getMyString(@StringRes resId: Int, vararg formatArgs: Any?) =
            instance.getString(resId, *formatArgs)
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        GlobalScope.launch(Dispatchers.IO) {
            initTheme()
        }

        GatewayApi.savedLldJsonFile = File(getDownloadDir(), GatewayApi.LLD_JSON_NAME)
    }

    private suspend fun initTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themesValue =
            sharedPreferences.getString(getMyString(R.string.interface_themes_key), "")!!
        if (themesValue.isNotEmpty()) {
            setMyTheme(themesValue)
        }
    }
}