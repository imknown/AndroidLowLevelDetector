package net.imknown.android.forefrontinfo

import android.os.StrictMode
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.LogRequestAsCurlInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.topjohnwu.superuser.Shell
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

        Shell.enableVerboseLogging = true

        FuelManager.instance.addRequestInterceptor(LogRequestAsCurlInterceptor)
        FuelManager.instance.addRequestInterceptor(LogRequestInterceptor)
        FuelManager.instance.addResponseInterceptor(LogResponseInterceptor)
    }
}