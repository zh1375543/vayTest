package com.vaycore.finance.loan.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_approvalDenied
import com.vaycore.finance.data.ACT_approvalInProgress
import com.vaycore.finance.data.ACT_index
import com.vaycore.finance.data.HomeLoanAmountRange
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.PageHomePre
import com.vaycore.finance.data.PageHomeRefuse
import com.vaycore.finance.data.bean.Event
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.home.data.DefaultHomeRepository
import com.vaycore.finance.home.data.HomeRepository
import com.vaycore.finance.home.state.CreditStage
import com.vaycore.finance.home.state.HomeEffect
import com.vaycore.finance.home.state.HomeEntryTracker
import com.vaycore.finance.home.state.MemberHomeUiState
import com.vaycore.finance.home.state.toMemberHomeUiState
import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.util.toJsonString
import kotlinx.coroutines.Job

/** Supplies the loan dashboard payload to loan- and repayment-related screens. */
class LoanDashboardViewModel(
    private val homeRepository: HomeRepository = DefaultHomeRepository(api),
) : BaseViewModel() {

    val authResult = MutableLiveData<LoanDashboardResponse?>()
    val memberHomeState = MutableLiveData<MemberHomeUiState>()
    val homeEffect = MutableLiveData<Event<HomeEffect>>()
    val loadFailedResult = MutableLiveData<Unit>()

    private var authJob: Job? = null

    fun getAuthData(isLoading: Boolean = false) {
        loadAuthData(isLoading = isLoading, renderMemberHome = false)
    }

    /** Loads the dashboard specifically for HomeFragment and publishes home-owned UI models. */
    fun getMemberHomeData(isLoading: Boolean = false) {
        loadAuthData(isLoading = isLoading, renderMemberHome = true)
    }

    private fun loadAuthData(isLoading: Boolean, renderMemberHome: Boolean) {
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
            if (renderMemberHome) {
                it?.let { response ->
                    val state = response.toMemberHomeUiState()
                    memberHomeState.value = state
                    trackCreditStage(state)
                    publishHomeEffects(state)
                }
            }
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

    private fun trackCreditStage(state: MemberHomeUiState) {
        val tracking = when (state.creditStage) {
            CreditStage.REVIEWING -> TrackBean(
                p = PageHomePre,
                act = ACT_approvalInProgress,
                result = System.currentTimeMillis().toString(),
            )

            CreditStage.REJECTED -> TrackBean(
                p = PageHomeRefuse,
                act = ACT_approvalDenied,
                result = System.currentTimeMillis().toString(),
            )

            CreditStage.APPROVED -> null
        }
        tracking?.let(::submitTrackingEvent)
    }

    private fun publishHomeEffects(state: MemberHomeUiState) {
        if (state.hasPendingRepayment) {
            emitHomeEffect(HomeEffect.ShowAppRating)
        }

        val shouldNavigateToOrders =
            state.hasRepaymentProducts && HomeEntryTracker.consumeFirstEntry()
        if (shouldNavigateToOrders) {
            emitHomeEffect(HomeEffect.NavigateToOrders)
            return
        }

        when {
            state.newProducts.isNotEmpty() -> {
                emitHomeEffect(HomeEffect.ShowNewProducts(state.newProducts))
            }

            state.canShowAvailableCreditDialog -> {
                emitHomeEffect(
                    HomeEffect.ShowAvailableCredit(
                        amount = state.availableAmount,
                        currencySymbol = state.creditCurrencySymbol ?: state.fallbackCurrencySymbol,
                    ),
                )
            }
        }
    }

    private fun emitHomeEffect(effect: HomeEffect) {
        homeEffect.value = Event(effect)
    }
}
