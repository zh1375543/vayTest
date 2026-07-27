package com.vaycore.finance.identity.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_OTPFail
import com.vaycore.finance.data.ACT_createPassword
import com.vaycore.finance.data.ACT_getVerifyCode
import com.vaycore.finance.data.ACT_loginOTP
import com.vaycore.finance.data.ACT_loginPassword
import com.vaycore.finance.data.PageCreatePassword
import com.vaycore.finance.data.PageLogin
import com.vaycore.finance.data.activityUrl
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.token
import com.vaycore.finance.identity.data.SessionRepository
import com.vaycore.finance.model.identity.LoginSessionResponse
import com.vaycore.finance.util.toJsonString

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository(api),
) : BaseViewModel() {

    val otpResult = MutableLiveData<Any?>()
    fun sendOTP(phone: String) {
        createNetworkRequest { sessionRepository.sendOTP(phone) }
            .showLoading().onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_getVerifyCode,
                        result = it.toJsonString()
                    )
                )
                otpResult.value = it
            }.onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_getVerifyCode,
                        result = it.toJsonString()
                    )
                )
                submitTrackingEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_OTPFail,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }

    val loginResult = MutableLiveData<LoginSessionResponse?>()
    fun login(
        phone: String,
        code: String?,
        password: String?,
    ) {
        createNetworkRequest {
            sessionRepository.login(phone, code, password)
        }.showLoading().onSuccess {
            it?.let {
                submitTrackingEvent(
                    TrackBean(
                        p = PageLogin,
                        act = if (password == null) ACT_loginOTP else ACT_loginPassword,
                        result = it.toJsonString()
                    )
                )
                token = it.token
                activityUrl = it.activityUrl.orEmpty()
                loginInfo = it
                loginResult.value = it
            }
        }.onFailed {
            submitTrackingEvent(
                TrackBean(
                    p = PageLogin,
                    act = if (password == null) ACT_loginOTP else ACT_loginPassword,
                    result = it.toJsonString()
                )
            )
            false
        }
    }

    fun postDeviceInfo() {
        createNetworkRequest {
            sessionRepository.postDeviceInfo()
        }.onSuccess { }.execute()
    }

    val logoutResult = MutableLiveData<Any?>()
    fun logout() {
        createNetworkRequest {
            sessionRepository.logout()
        }.showLoading().onSuccess {
            logoutResult.value = it
        }.execute()
    }

    val sendChangePasswordOtpResult = MutableLiveData<Any?>()
    fun sendChangePasswordOTP(phone: String) {
        createNetworkRequest { sessionRepository.sendOTP(phone) }.showLoading().onSuccess {
            sendChangePasswordOtpResult.value = it
        }.execute()
    }

    val changeResult = MutableLiveData<LoginSessionResponse?>()
    fun changePassword(phone: String, code: String, password: String) {
        createNetworkRequest {
            sessionRepository.changePassword(phone, code, password)
        }.showLoading().onSuccess {
            changeResult.value = it
        }.execute()
    }

    val setPwdResult = MutableLiveData<LoginSessionResponse?>()
    fun setPassword(phone: String, password: String) {
        createNetworkRequest {
            sessionRepository.setPassword(phone, password)
        }.showLoading().onSuccess {
            submitTrackingEvent(
                TrackBean(
                    p = PageCreatePassword,
                    act = ACT_createPassword,
                    result = it.toJsonString()
                )
            )
            setPwdResult.value = it
        }.onFailed {
            submitTrackingEvent(
                TrackBean(
                    p = PageCreatePassword,
                    act = ACT_createPassword,
                    result = it.toJsonString()
                )
            )
            false
        }
    }
}