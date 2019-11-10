package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
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
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        GatewayApi.savedLldJsonFile = File(getDownloadDir(), GatewayApi.LLD_JSON_NAME)
    }
}