package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
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

        internal suspend fun setTheme(themesValue: String) {
            val clazz = AppCompatDelegate::class.java
            val field = clazz.getDeclaredField(themesValue)
            @AppCompatDelegate.NightMode val mode = field.getInt(null)

            withContext(Dispatchers.Main) {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        GatewayApi.savedLldJsonFile = File(getDownloadDir(), GatewayApi.LLD_JSON_NAME)
    }
}