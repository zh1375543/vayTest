package com.vaycore.finance.order

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.model.order.LoanOrderDetailResponse
import com.vaycore.finance.model.order.OrderBean
import com.vaycore.finance.model.repayment.RepaymentActionResponse

class LoanOrderViewModel(
    private val loanOrderRepository: LoanOrderRepository = LoanOrderRepository(BaseViewModel.api),
) : BaseViewModel() {

    val orderListResult = MutableLiveData<List<OrderBean>>()
    fun getOrderList(errorAction: () -> Unit) {
        createNetworkRequest {
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
        createNetworkRequest {
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
        createNetworkRequest {
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
        createNetworkRequest {
            loanOrderRepository.installmentRepay(orderNo, planNumberList)
        }.showLoading().onSuccess {
            installmentRepayResult.value = it
        }.execute()
    }

    fun repayAndBorrow(id: Long?, applyAgainSign: Int?, block: () -> Unit) {
        createNetworkRequest {
            loanOrderRepository.repayAndBorrow(id, applyAgainSign)
        }.showLoading().onSuccess {
            block()
        }.execute()
    }

    fun cancelApply(id: Long?, block: () -> Unit) {
        createNetworkRequest {
            loanOrderRepository.cancelApply(id)
        }.showLoading().onSuccess {
            block()
        }.execute()
    }
}
