package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_LoanAppProductInfoDetail
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.repository.LoanProductRepository
import com.vaycore.finance.util.toJsonString

class LoanProductViewModel(
    private val loanProductRepository: LoanProductRepository = LoanProductRepository(BaseViewModel.api),
) : BaseViewModel() {

    val detailResult = MutableLiveData<ProductBean?>()
    fun getProductDetail(
        trackPage: String,
        id: String?,
        amount: String?,
        showLoading: Boolean = false,
        errorAction: () -> Unit,
    ) {
        launchData {
            loanProductRepository.fetchProductDetail(
                productId = id,
                amount = amount,
            )
        }.showLoading(showLoading).onSuccess {
            recordEvent(
                TrackBean(
                    p = trackPage,
                    act = ACT_LoanAppProductInfoDetail,
                    result = it.toJsonString()
                )
            )
            detailResult.value = it
        }.onFailed {
            recordEvent(
                TrackBean(
                    p = trackPage,
                    act = ACT_LoanAppProductInfoDetail,
                    result = it.toJsonString()
                )
            )
            errorAction.invoke()
            false
        }
    }
}
