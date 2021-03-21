package net.imknown.android.forefrontinfo.ui.home

import net.imknown.android.forefrontinfo.ui.home.datasource.IHomeDataSource

class HomeRepository(
    private val homeLocalDataSource: IHomeDataSource,
    private val homeGatewayDataSource: IHomeDataSource
) : IHomeRepository {
    override suspend fun fetchLldJson() = homeGatewayDataSource.fetchLldJson()
}