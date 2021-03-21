package net.imknown.android.forefrontinfo.ui.home.datasource

import android.content.Context
import androidx.annotation.AttrRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.common.isGoEdition
import net.imknown.android.forefrontinfo.ui.common.isLatestPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isLatestStableAndroid
import net.imknown.android.forefrontinfo.ui.common.isSupportedByUpstreamAndroid
import net.imknown.android.forefrontinfo.ui.home.model.Lld

class BuildIdDataSource {
    companion object {
        private const val BUILD_ID_SEPARATOR = '.'

        private const val PROP_RO_SYSTEM_BUILD_ID = "ro.system.build.id"
        private const val PROP_RO_VENDOR_BUILD_ID = "ro.vendor.build.id"
        private const val PROP_RO_ODM_BUILD_ID = "ro.odm.build.id"
    }

    @AttrRes
    fun getAndroidColor(lld: Lld) = when {
        isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorNoProblem
        isSupportedByUpstreamAndroid(lld) -> R.attr.colorWaring
        else -> R.attr.colorCritical
    }

    fun isGoEdition(context: Context) = context.isGoEdition()
}