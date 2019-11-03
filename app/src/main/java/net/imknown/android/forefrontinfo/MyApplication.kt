package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode

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
}