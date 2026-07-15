package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.OrderBean
import com.vaycore.finance.data.local.bean.LoanOrderDetailResponse
import com.vaycore.finance.data.local.bean.RepaymentActionResponse
import com.vaycore.finance.data.repository.LoanOrderRepository

class LoanOrderViewModel(
    private val loanOrderRepository: LoanOrderRepository = LoanOrderRepository(BaseViewModel.api),
) : BaseViewModel() {

    val orderListResult = MutableLiveData<List<OrderBean>>()
    fun getOrderList(errorAction: () -> Unit) {
        launchData {
            loanOrderRepository.fetchOrderList()
        }.onSuccess {
            orderListResult.value = it ?: emptyList()
        }.onFailed {
            errorAction.invoke()
            true
        }
    }

    val orderDetailResult = MutableLiveData<LoanOrderDetailResponse?>()
    fun getOrderDetail(orderId: Long?, errorAction: () -> Unit) {
        launchData {
            loanOrderRepository.fetchOrderDetail(orderId)
        }.onSuccess {
            orderDetailResult.value = it
        }.onFailed {
            errorAction.invoke()
            true
        }
    }

    val buttonResult = MutableLiveData<String?>()
    fun getButtonState() {
        launchData {
            loanOrderRepository.fetchRepaymentBorrowButtonState()
        }.onSuccess {
            buttonResult.value = it
        }.onFailed {
            buttonResult.value = null
            false
        }
    }

    val installmentRepayResult = MutableLiveData<RepaymentActionResponse?>()
    fun installmentRepay(orderNo: String?, planNumberList: List<Int?>?) {
        launchData {
            loanOrderRepository.installmentRepay(orderNo, planNumberList)
        }.showLoading().onSuccess {
            installmentRepayResult.value = it
        }.execute()
    }

    fun repayAndBorrow(id: Long?, block: () -> Unit) {
        launchData {
            loanOrderRepository.repayAndBorrow(id)
        }.showLoading().onSuccess {
            block()
        }.execute()
    }
}
