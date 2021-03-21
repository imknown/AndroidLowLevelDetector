package net.imknown.android.forefrontinfo.ui.home.datasource

import android.content.Context
import androidx.annotation.AttrRes
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.common.isGoEdition
import net.imknown.android.forefrontinfo.ui.common.isLatestPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isLatestStableAndroid
import net.imknown.android.forefrontinfo.ui.common.isSupportedByUpstreamAndroid
import net.imknown.android.forefrontinfo.ui.home.model.Lld

class AndroidDataSource {
    @AttrRes
    fun getAndroidColor(lld: Lld) = when {
        isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorNoProblem
        isSupportedByUpstreamAndroid(lld) -> R.attr.colorWaring
        else -> R.attr.colorCritical
    }

    fun isGoEdition(context: Context) = context.isGoEdition()
}