package com.vaycore.finance.repayment

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.model.repayment.RepaymentActionResponse
import com.vaycore.finance.util.formatAmountWithPrefix
import java.math.BigDecimal

class RepayViewModel(
    private val repaymentRepository: RepaymentRepository = RepaymentRepository(BaseViewModel.api),
) : BaseViewModel() {

    val accountsResult = MutableLiveData<List<BankAccountResponse>?>()
    fun getRepayBankList(errorAction: () -> Unit) {
        launchData {
            repaymentRepository.fetchRepayBankList()
        }.onSuccess {
            accountsResult.value = it
        }.onFailed {
            errorAction()
            false
        }
    }

    val cardListResult = MutableLiveData<MutableList<BankAccountResponse>?>()
    fun getRepayCardList() {
        launchData {
            repaymentRepository.fetchRepayCardList()
        }.showLoading().onSuccess {
            cardListResult.value = it
        }.execute()
    }

    val repayResult = MutableLiveData<Any?>()
    fun repayment(
        imagCert: Uri,
        orderId: String,
        repayInfoId: String?,
        repayType: String?,
    ) {
        launchData {
            repaymentRepository.uploadRepaymentVoucher(
                imageCert = imagCert,
                orderId = orderId,
                repayInfoId = repayInfoId,
                repayType = repayType,
            )
        }.showLoading().onSuccess {
            repayResult.value = it
        }.execute()
    }

    val orderListResult = MutableLiveData<List<ProductBean>?>()
    val selectedOrderCount = MutableLiveData("0")
    val selectedOrderAmount = MutableLiveData("0")

    fun getOrderList(errorAction: () -> Unit) {
        launchData {
            repaymentRepository.fetchBatchRepaymentOrders()
        }.onSuccess {
            val orders = it.orEmpty().onEach { order -> order.isCheck = true }
            updateBatchSelection(orders)
            orderListResult.value = orders
        }.onFailed {
            errorAction()
            true
        }
    }

    fun updateBatchSelection(orders: List<ProductBean>) {
        val selectedOrders = orders.filter(ProductBean::isCheck)
        selectedOrderCount.value = selectedOrders.size.toString()
        selectedOrderAmount.value = if (orders.isEmpty()) {
            "0"
        } else {
            selectedOrders.fold(BigDecimal.ZERO) { total, order ->
                total + (order.actualRepayAmount ?: BigDecimal.ZERO)
            }.formatAmountWithPrefix((selectedOrders.firstOrNull() ?: orders.first()).currencySymbol)
        }
    }

    val togetherRepayResult = MutableLiveData<RepaymentActionResponse?>()
    fun togetherRepayment(orderList: List<String>) {
        launchData {
            repaymentRepository.submitBatchRepayment(orderList)
        }.onSuccess {
            togetherRepayResult.value = it
        }.execute()
    }
}
