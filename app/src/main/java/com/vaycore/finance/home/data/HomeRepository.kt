package com.vaycore.finance.home.data

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.model.home.CampaignBannerResponse
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.model.loan.LoanDashboardResponse

interface HomeRepository {
    suspend fun loadGuestHome(): GuestHomeResponse

    suspend fun loadMemberHome(): LoanDashboardResponse

    suspend fun loadBanners(): List<CampaignBannerResponse>
}

class DefaultHomeRepository(
    private val api: Api,
) : HomeRepository {

    override suspend fun loadGuestHome(): GuestHomeResponse {
        return requireNotNull(api.fetchHomeData(ApiRequest()).dataOrThrow())
    }

    override suspend fun loadMemberHome(): LoanDashboardResponse {
        return requireNotNull(api.fetchHomeLoan(ApiRequest()).dataOrThrow())
    }

    override suspend fun loadBanners(): List<CampaignBannerResponse> {
        return api.fetchBannerList().dataOrThrow().orEmpty()
    }
}
