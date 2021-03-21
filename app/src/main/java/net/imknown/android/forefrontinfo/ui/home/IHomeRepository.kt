package net.imknown.android.forefrontinfo.ui.home

import net.imknown.android.forefrontinfo.ui.base.IRepository

interface IHomeRepository : IRepository {
    @Throws
    suspend fun fetchLldJson(): String
}