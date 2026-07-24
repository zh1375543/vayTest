package com.vaycore.finance.loan

import com.vaycore.finance.model.home.CampaignBannerResponse
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.model.identity.UserAuthStatusResponse
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow

class LoanDashboardRepository(
    private val api: Api,
) {

    suspend fun fetchUserAuthStatus(): UserAuthStatusResponse? {
        return api.fetchUserAuth(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchGuestDashboard(): GuestHomeResponse? {
        return api.fetchHomeData(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchLoanDashboard(): LoanDashboardResponse? {
        return api.fetchHomeLoan(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchBannerList(): List<CampaignBannerResponse> {
        return api.fetchBannerList().dataOrThrow() ?: emptyList()
    }

    suspend fun fetchAuthConfigList(): List<String> {
        return api.fetchAuthentication()
            .dataOrThrow()
            ?.authConfig
            ?.split(",")
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    suspend fun submitFeedback(content: String): Any? {
        return api.submitFeedback(ApiRequest(content = content)).dataOrThrow()
    }
}
