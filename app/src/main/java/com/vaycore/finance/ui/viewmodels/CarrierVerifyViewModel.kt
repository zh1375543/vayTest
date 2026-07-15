package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_getVerifyCode
import com.vaycore.finance.data.ACT_nextStep
import com.vaycore.finance.data.PageVerifyCode
import com.vaycore.finance.data.PageVerifyCode2
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.repository.IdentityVerificationRepository
import com.vaycore.finance.util.toJsonString

class CarrierVerifyViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(BaseViewModel.api),
) : BaseViewModel() {

    val getOtpResult = MutableLiveData<Any?>()
    fun getOtp(phone: String, company: String) {
        launchData { verificationRepository.requestCarrierOtp(phone, company) }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageVerifyCode, act = ACT_getVerifyCode, result = it.toJsonString()))
                getOtpResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageVerifyCode, act = ACT_getVerifyCode, result = it.toJsonString()))
                false
            }
    }

    val submitOtpResult = MutableLiveData<Any?>()
    fun submitOtp(phone: String, company: String, otp: String) {
        launchData { verificationRepository.submitCarrierOtp(phone, company, otp) }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageVerifyCode2, act = ACT_nextStep, result = it.toJsonString()))
                submitOtpResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageVerifyCode2, act = ACT_nextStep, result = it.toJsonString()))
                false
            }
    }

    val submitWithGetOtpResult = MutableLiveData<Any?>()
    fun submitWithGetOtp(phone: String, company: String, otp: String) {
        launchData { verificationRepository.submitCarrierOtpAndRequestNext(phone, company, otp) }
            .showLoading()
            .onSuccess { submitWithGetOtpResult.value = it }
            .execute()
    }
}
