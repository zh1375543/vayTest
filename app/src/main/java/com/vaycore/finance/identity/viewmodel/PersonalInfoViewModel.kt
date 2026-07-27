package com.vaycore.finance.identity.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_next
import com.vaycore.finance.data.PageInfoPersonal
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.bean.SelectionOption
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.identity.data.IdentityVerificationRepository
import com.vaycore.finance.model.identity.PersonalProfileOptionsResponse
import com.vaycore.finance.model.identity.PersonalProfileResponse
import com.vaycore.finance.model.identity.WorkProfileOptionsResponse
import com.vaycore.finance.util.toJsonString

class PersonalInfoViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(api),
) : BaseViewModel() {

    val submitResult = MutableLiveData<Any?>()
    fun submitPersonalInfo(paramBean: ApiRequest) {
        createNetworkRequest { verificationRepository.submitPersonalInfo(paramBean) }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_next,
                        result = it.toJsonString()
                    )
                )
                submitResult.value = it
            }
            .onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoPersonal,
                        act = ACT_next,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }

    private val enumBean = MutableLiveData<PersonalProfileOptionsResponse?>()
    fun getEnums(action: (PersonalProfileOptionsResponse) -> Unit) {
        enumBean.value?.let {
            action(it)
            return
        }
        createNetworkRequest { verificationRepository.fetchPersonalInfoOptions() }
            .showLoading()
            .onSuccess {
                enumBean.value = it
                it?.let(action)
            }
            .execute()
    }

    fun getAddressList(id: String? = null, action: (List<SelectionOption>) -> Unit) {
        createNetworkRequest { verificationRepository.fetchAddressOptions(id) }
            .showLoading()
            .onSuccess { action(it ?: emptyList()) }
            .execute()
    }

    fun getWorkInfoOptions(action: (WorkProfileOptionsResponse) -> Unit) {
        createNetworkRequest { verificationRepository.fetchWorkInfoOptions() }
            .showLoading()
            .onSuccess { it?.let(action) }
            .execute()
    }

    val personalResult = MutableLiveData<PersonalProfileResponse?>()
    fun getPersonalInfo(errorAction: () -> Unit) {
        createNetworkRequest { verificationRepository.fetchPersonalInfo() }
            .onSuccess { personalResult.value = it }
            .onFailed {
                errorAction()
                false
            }
    }
}