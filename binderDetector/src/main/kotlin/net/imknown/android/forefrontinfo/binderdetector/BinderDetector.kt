package net.imknown.android.forefrontinfo.binderdetector

object BinderDetector {
    const val PREFIX = "BinderDetector"

    external fun getBinderVersion(driver: String): Int
}