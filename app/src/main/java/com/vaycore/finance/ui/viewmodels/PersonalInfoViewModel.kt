package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_next
import com.vaycore.finance.data.PageInfoPersonal
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.PersonalProfileOptionsResponse
import com.vaycore.finance.data.local.bean.PersonalProfileResponse
import com.vaycore.finance.data.local.bean.SelectionOption
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.bean.WorkProfileOptionsResponse
import com.vaycore.finance.data.repository.IdentityVerificationRepository
import com.vaycore.finance.util.toJsonString

class PersonalInfoViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(BaseViewModel.api),
) : BaseViewModel() {

    val submitResult = MutableLiveData<Any?>()
    fun submitPersonalInfo(paramBean: ApiRequest) {
        launchData { verificationRepository.submitPersonalInfo(paramBean) }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageInfoPersonal, act = ACT_next, result = it.toJsonString()))
                submitResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageInfoPersonal, act = ACT_next, result = it.toJsonString()))
                false
            }
    }

    private val enumBean = MutableLiveData<PersonalProfileOptionsResponse?>()
    fun getEnums(action: (PersonalProfileOptionsResponse) -> Unit) {
        enumBean.value?.let {
            action(it)
            return
        }
        launchData { verificationRepository.fetchPersonalInfoOptions() }
            .showLoading()
            .onSuccess {
                enumBean.value = it
                it?.let(action)
            }
            .execute()
    }

    fun getAddressList(id: String? = null, action: (List<SelectionOption>) -> Unit) {
        launchData { verificationRepository.fetchAddressOptions(id) }
            .showLoading()
            .onSuccess { action(it ?: emptyList()) }
            .execute()
    }

    fun getWorkInfoOptions(action: (WorkProfileOptionsResponse) -> Unit) {
        launchData { verificationRepository.fetchWorkInfoOptions() }
            .showLoading()
            .onSuccess { it?.let(action) }
            .execute()
    }

    val personalResult = MutableLiveData<PersonalProfileResponse?>()
    fun getPersonalInfo(errorAction: () -> Unit) {
        launchData { verificationRepository.fetchPersonalInfo() }
            .onSuccess { personalResult.value = it }
            .onFailed {
                errorAction()
                false
            }
    }
}
