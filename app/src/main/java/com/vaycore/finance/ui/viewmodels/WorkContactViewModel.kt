package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_next
import com.vaycore.finance.data.PageInfoBank
import com.vaycore.finance.data.PageSupplementaryInformation
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.bean.WorkContactProfileResponse
import com.vaycore.finance.data.local.bean.WorkProfileOptionsResponse
import com.vaycore.finance.data.repository.IdentityVerificationRepository
import com.vaycore.finance.util.toJsonString

class WorkContactViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(BaseViewModel.api),
) : BaseViewModel() {

    fun getContactEnum(action: (WorkProfileOptionsResponse) -> Unit) {
        launchData { verificationRepository.fetchWorkInfoOptions() }
            .showLoading()
            .onSuccess { it?.let(action) }
            .execute()
    }

    val contractResult = MutableLiveData<WorkContactProfileResponse?>()
    fun getContactsInfo(errorAction: () -> Unit = {}) {
        launchData { verificationRepository.fetchContactInfo() }
            .onSuccess { contractResult.value = it }
            .onFailed {
                errorAction()
                true
            }
    }

    val submitBankAndCtsResult = MutableLiveData<Any?>()
    fun submitBankAndCtsInfo(paramBean: ApiRequest) {
        launchData { verificationRepository.submitBankAndContactInfo(paramBean) }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageInfoBank, act = ACT_next, result = it.toJsonString()))
                submitBankAndCtsResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageInfoBank, act = ACT_next, result = it.toJsonString()))
                false
            }
    }

    val submitSuppleInfoResult = MutableLiveData<Any?>()
    fun submitSuppleInfo(paramBean: ApiRequest) {
        launchData { verificationRepository.submitSupplementInfo(paramBean) }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageSupplementaryInformation, act = ACT_next, result = it.toJsonString()))
                submitSuppleInfoResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageSupplementaryInformation, act = ACT_next, result = it.toJsonString()))
                false
            }
    }
}
