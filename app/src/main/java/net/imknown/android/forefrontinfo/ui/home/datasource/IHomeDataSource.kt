package net.imknown.android.forefrontinfo.ui.home.datasource

interface IHomeDataSource {
    suspend fun fetchLldJson(): String
}