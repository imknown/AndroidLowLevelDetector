package net.imknown.android.forefrontinfo.ui.others.datasource

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import androidx.annotation.RequiresApi

class RomDataSource {
    fun getUser(): String = Build.USER
    fun getHost(): String = Build.HOST
    fun getTime(): Long = Build.TIME
    @RequiresApi(Build.VERSION_CODES.M)
    fun getBaseOs(): String = Build.VERSION.BASE_OS

    fun getId(): String = Build.ID
    fun getDisplay(): String = Build.DISPLAY
    fun getType(): String = Build.TYPE
    fun getTags(): String = Build.TAGS
    fun getIncremental(): String = Build.VERSION.INCREMENTAL
    fun getCodename(): String = Build.VERSION.CODENAME
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getReleaseOrPreviewDisplay(): String = Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
    @RequiresApi(Build.VERSION_CODES.M)
    fun getPreviewSdkInt(): Int = Build.VERSION.PREVIEW_SDK_INT

    fun getDefaultUserAgentOrThrow(context: Context): String = WebSettings.getDefaultUserAgent(context)
}