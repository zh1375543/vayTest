package com.vaycore.finance.loan.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_index
import com.vaycore.finance.data.HomeLoanAmountRange
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.home.data.DefaultHomeRepository
import com.vaycore.finance.home.data.HomeRepository
import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.util.toJsonString
import kotlinx.coroutines.Job

/** Supplies the loan dashboard payload to loan- and repayment-related screens. */
class LoanDashboardViewModel(
    private val homeRepository: HomeRepository = DefaultHomeRepository(api),
) : BaseViewModel() {

    val authResult = MutableLiveData<LoanDashboardResponse?>()
    val loadFailedResult = MutableLiveData<Unit>()

    private var authJob: Job? = null

    fun getAuthData(isLoading: Boolean = false) {
        authJob?.cancel()
        authJob = createNetworkRequest {
            homeRepository.loadMemberHome()
        }.showLoading(isLoading).onSuccess {
            HomeLoanAmountRange = it?.loanAmountRange
            submitTrackingEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_index,
                    result = it.toJsonString(),
                ),
            )
            authResult.value = it
        }.onFailed {
            loadFailedResult.value = Unit
            submitTrackingEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_index,
                    result = it.toJsonString(),
                ),
            )
            true
        }
    }
}
