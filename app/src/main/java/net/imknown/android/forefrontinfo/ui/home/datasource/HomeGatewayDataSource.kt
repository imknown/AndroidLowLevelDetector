package net.imknown.android.forefrontinfo.ui.home.datasource

import net.imknown.android.forefrontinfo.ui.home.GatewayApi

class HomeGatewayDataSource : IHomeDataSource {
    override suspend fun fetchLldJson() = GatewayApi.fetchLldJson()
}