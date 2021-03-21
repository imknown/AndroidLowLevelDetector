package net.imknown.android.forefrontinfo.ui.home

import net.imknown.android.forefrontinfo.ui.home.datasource.IHomeDataSource

class HomeRepository(
    homeLocalDataSource: IHomeDataSource,
    homeGatewayDataSource: IHomeDataSource
) : IHomeRepository {
    override suspend fun fetchLldJson() = GatewayApi.fetchLldJson()
}