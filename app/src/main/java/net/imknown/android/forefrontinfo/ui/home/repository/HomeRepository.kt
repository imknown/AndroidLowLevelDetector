package net.imknown.android.forefrontinfo.ui.home.repository

import android.content.Context
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.BuildIdDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.model.Lld

class HomeRepository(
    private val lldDataSource: LldDataSource,
    private val androidDataSource: AndroidDataSource,
    private val buildIdDataSource: BuildIdDataSource,
) {
    fun fetchOfflineLldOrThrow() = lldDataSource.fetchOfflineLldOrThrow()
    fun fetchOnlineLldOrThrow() = lldDataSource.fetchOnlineLldJsonString().toObjectOrThrow<Lld>()

    fun getAndroidColor(lld: Lld) = androidDataSource.getAndroidColor(lld)

    fun isGoEdition(context: Context) = androidDataSource.isGoEdition(context)
}