package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Environment
import java.io.File

@SuppressLint("Registered")
open class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        GatewayApi.savedFile = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: filesDir.resolve(Environment.DIRECTORY_DOWNLOADS),
            GatewayApi.LLD_JSON_NAME
        )
    }
}