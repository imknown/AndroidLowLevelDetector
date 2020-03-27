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

        initStrictMode()
    }

    private fun initStrictMode() {
        // StrictMode.enableDefaults()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .also {
                    if (isAtLeastStableAndroid6()) {
                        it.detectResourceMismatches()
                    }
                }.also {
                    if (isAtLeastStableAndroid8()) {
                        it.detectUnbufferedIo()
                    }
                }
                .penaltyLog()
                .penaltyDropBox()
                .penaltyDialog()
                .penaltyFlashScreen()
//                .penaltyDeath()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects()
                .also {
                    if (isAtLeastStableAndroid6()) {
                        it.detectCleartextNetwork()
                    }
                }.also {
                    if (isAtLeastStableAndroid8()) {
                        it.detectContentUriWithoutPermission()
                        it.detectUntaggedSockets()
                    }
//                }.also {
//                    if (isAtLeastAndroid9()) {
//                        it.detectNonSdkApiUsage()
//                    }
                }.also {
                    if (isAtLeastStableAndroid10()) {
                        it.detectImplicitDirectBoot()
                        it.detectCredentialProtectedWhileLocked()
                    }
                }
                .penaltyLog()
//                .penaltyDeath()
                .penaltyDropBox()
                .build()
        )
    }
}