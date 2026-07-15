package com.vaycore.finance.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.RepaymentActionResponse
import com.vaycore.finance.data.repository.RepaymentRepository

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
    fun getOrderList(errorAction: () -> Unit) {
        launchData {
            repaymentRepository.fetchBatchRepaymentOrders()
        }.onSuccess {
            orderListResult.value = it
        }.onFailed {
            errorAction()
            true
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
