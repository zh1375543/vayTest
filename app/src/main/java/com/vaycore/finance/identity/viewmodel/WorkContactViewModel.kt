package com.vaycore.finance.identity.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_next
import com.vaycore.finance.data.PageInfoBank
import com.vaycore.finance.data.PageSupplementaryInformation
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.identity.data.IdentityVerificationRepository
import com.vaycore.finance.model.identity.WorkContactProfileResponse
import com.vaycore.finance.model.identity.WorkProfileOptionsResponse
import com.vaycore.finance.util.toJsonString

class WorkContactViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(api),
) : BaseViewModel() {

    fun getContactEnum(action: (WorkProfileOptionsResponse) -> Unit) {
        createNetworkRequest { verificationRepository.fetchWorkInfoOptions() }
            .showLoading()
            .onSuccess { it?.let(action) }
            .execute()
    }

    val contractResult = MutableLiveData<WorkContactProfileResponse?>()
    fun getContactsInfo(errorAction: () -> Unit = {}) {
        createNetworkRequest { verificationRepository.fetchContactInfo() }
            .onSuccess { contractResult.value = it }
            .onFailed {
                errorAction()
                true
            }
    }

    val submitBankAndCtsResult = MutableLiveData<Any?>()
    fun submitBankAndCtsInfo(paramBean: ApiRequest) {
        createNetworkRequest { verificationRepository.submitBankAndContactInfo(paramBean) }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(TrackBean(p = PageInfoBank, act = ACT_next, result = it.toJsonString()))
                submitBankAndCtsResult.value = it
            }
            .onFailed {
                submitTrackingEvent(TrackBean(p = PageInfoBank, act = ACT_next, result = it.toJsonString()))
                false
            }
    }

    val submitSuppleInfoResult = MutableLiveData<Any?>()
    fun submitSuppleInfo(paramBean: ApiRequest) {
        createNetworkRequest { verificationRepository.submitSupplementInfo(paramBean) }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageSupplementaryInformation,
                        act = ACT_next,
                        result = it.toJsonString()
                    )
                )
                submitSuppleInfoResult.value = it
            }
            .onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageSupplementaryInformation,
                        act = ACT_next,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }
}