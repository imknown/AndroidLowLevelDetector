package net.imknown.android.forefrontinfo

import android.os.StrictMode
import leakcanary.LeakCanaryProcess

class MyDebugApplication : MyApplication() {
    override fun onCreate() {
        super.onCreate()

        if (LeakCanaryProcess.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        StrictMode.enableDefaults()
    }
}