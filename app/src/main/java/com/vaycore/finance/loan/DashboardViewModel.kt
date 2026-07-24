package com.vaycore.finance.loan

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_common
import com.vaycore.finance.data.ACT_index
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.HomeLoanAmountRange
import com.vaycore.finance.model.home.CampaignBannerResponse
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.model.identity.UserAuthStatusResponse
import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.util.toJsonString
import kotlinx.coroutines.Job

class DashboardViewModel(
    private val dashboardRepository: LoanDashboardRepository = LoanDashboardRepository(api),
) : BaseViewModel() {

    companion object {
        var isFirstEnter = true
    }

    val userAuthStatusResult = MutableLiveData<UserAuthStatusResponse?>()
    val authFailedResult = MutableLiveData<Boolean>()
    fun getUserAuthStatus(errorAction: () -> Unit = {}) {
        launchData {
            dashboardRepository.fetchUserAuthStatus()
        }.onSuccess {
            userAuthStatusResult.value = it
        }.onFailed {
            authFailedResult.value = true
            errorAction()
            false
        }
    }

    val unAuthResult = MutableLiveData<GuestHomeResponse?>()
    fun getUnAuthData(showLoading: Boolean = false) {
        launchData {
            dashboardRepository.fetchGuestDashboard()
        }.showLoading(showLoading).onSuccess {
            unAuthResult.value = it
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString()
                )
            )
        }.onFailed {
            authFailedResult.value = false
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString()
                )
            )
            true
        }
    }

    val authResult = MutableLiveData<LoanDashboardResponse?>()
    private var authJob: Job? = null
    fun getAuthData(isLoading: Boolean = false) {
        authJob?.cancel()
        authJob = launchData {
            dashboardRepository.fetchLoanDashboard()
        }.showLoading(isLoading).onSuccess {
            HomeLoanAmountRange = it?.loanAmountRange
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_index,
                    result = it.toJsonString()
                )
            )
            authResult.value = it
        }.onFailed {
            authFailedResult.value = false
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_index,
                    result = it.toJsonString()
                )
            )
            true
        }
    }

    val bannerResult = MutableLiveData<List<CampaignBannerResponse>>()
    fun getBannerList() {
        launchData {
            dashboardRepository.fetchBannerList()
        }.onSuccess {
            bannerResult.value = it ?: listOf()
        }.onFailed { true }
    }

    fun fetchAuthConfigList(action: (List<String>) -> Unit) {
        if (!isLogin) return
        launchData {
            dashboardRepository.fetchAuthConfigList()
        }.onSuccess { list ->
            action(list.orEmpty())
        }.execute()
    }

    fun submitFeed(content: String, action: () -> Unit) {
        launchData {
            dashboardRepository.submitFeedback(content)
        }.showLoading().onSuccess {
            action()
        }.execute()
    }
}