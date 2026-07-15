package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_apply
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.PageProductDetail
import com.vaycore.finance.data.local.bean.LoanDashboardResponse
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.repository.LoanRepository
import com.vaycore.finance.util.toJsonString
import okhttp3.MultipartBody
import okhttp3.RequestBody

class LoanApplyViewModel(
    private val loanRepository: LoanRepository = LoanRepository(BaseViewModel.api),
) : BaseViewModel() {

    val togetherLoanResult = MutableLiveData<List<ProductBean>?>()
    fun togetherLoan(
        files: List<MultipartBody.Part>,
        multipartBody: Map<String, RequestBody>,
    ) {
        launchData {
            loanRepository.submitTogetherLoan(files, multipartBody)
        }.onSuccess {
            togetherLoanResult.value = it
            recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
        }.onFailed {
            loanFailResult.value = true
            recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
            true
        }
    }

    val loanFailResult = MutableLiveData<Boolean>()
    val loanResult = MutableLiveData<ProductBean?>()
    fun loan(
        files: List<MultipartBody.Part>,
        multipartBody: Map<String, RequestBody>,
    ) {
        launchData {
            loanRepository.submitLoan(files, multipartBody)
        }.onSuccess {
            recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
            loanResult.value = it
        }.onFailed {
            recordEvent(
                TrackBean(
                    p = PageProductDetail,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
            loanFailResult.value = true
            true
        }
    }

    val togetherInfo = MutableLiveData<LoanDashboardResponse?>()
    fun getTogetherLoan(errorAction: () -> Unit) {
        launchData {
            loanRepository.fetchTogetherLoan()
        }.onSuccess {
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
            togetherInfo.value = it
        }.onFailed {
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_apply,
                    result = it.toJsonString()
                )
            )
            errorAction.invoke()
            true
        }
    }
}
