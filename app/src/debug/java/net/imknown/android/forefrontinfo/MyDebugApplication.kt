package net.imknown.android.forefrontinfo

import android.annotation.SuppressLint
import android.os.StrictMode
import leakcanary.LeakCanaryProcess
import net.imknown.android.forefrontinfo.ui.base.IAndroidVersion

@SuppressLint("Registered")
class MyDebugApplication : MyApplication(), IAndroidVersion {
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