package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
import android.os.StrictMode
import java.io.File

@SuppressLint("Registered")
open class MyApplication : Application() {
    companion object {
        /**
         * https://stackoverflow.com/questions/49841781/strictmode-strictmodediskreadviolation-when-creating-sharedpreference
         */
        fun <T> allowReads(block: () -> T) {
            val oldPolicy = StrictMode.allowThreadDiskReads()
            try {
                block()
            } finally {
                StrictMode.setThreadPolicy(oldPolicy)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        GatewayApi.savedFile = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: filesDir.resolve(Environment.DIRECTORY_DOWNLOADS),
            GatewayApi.LLD_JSON_NAME
        )
    }
}