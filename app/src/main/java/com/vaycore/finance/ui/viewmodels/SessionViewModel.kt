package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_OTPFail
import com.vaycore.finance.data.ACT_createPassword
import com.vaycore.finance.data.ACT_getVerifyCode
import com.vaycore.finance.data.ACT_loginOTP
import com.vaycore.finance.data.ACT_loginPassword
import com.vaycore.finance.data.PageCreatePassword
import com.vaycore.finance.data.PageLogin
import com.vaycore.finance.data.local.bean.LoginSessionResponse
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.data.repository.SessionRepository
import com.vaycore.finance.data.local.token
import com.vaycore.finance.util.toJsonString

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository(BaseViewModel.api),
) : BaseViewModel() {

    val otpResult = MutableLiveData<Any?>()
    fun sendOTP(phone: String) {
        launchData { sessionRepository.sendOTP(phone) }
            .showLoading().onSuccess {
                recordEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_getVerifyCode,
                        result = it.toJsonString()
                    )
                )
                otpResult.value = it
            }.onFailed {
                recordEvent(
                    TrackBean(
                        p = PageLogin,
                        act = ACT_getVerifyCode,
                        result = it.toJsonString()
                    )
                )
                recordEvent(
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
        launchData {
            sessionRepository.login(phone, code, password)
        }.showLoading().onSuccess {
            it?.let {
                recordEvent(
                    TrackBean(
                        p = PageLogin,
                        act = if (password == null) ACT_loginOTP else ACT_loginPassword,
                        result = it.toJsonString()
                    )
                )
                token = it.token
                loginInfo = it
                loginResult.value = it
            }
        }.onFailed {
            recordEvent(
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
        launchData {
            sessionRepository.postDeviceInfo()
        }.onSuccess { }.execute()
    }

    val logoutResult = MutableLiveData<Any?>()
    fun logout() {
        launchData {
            sessionRepository.logout()
        }.showLoading().onSuccess {
            logoutResult.value = it
        }.execute()
    }

    val sendChangePasswordOtpResult = MutableLiveData<Any?>()
    fun sendChangePasswordOTP(phone: String) {
        launchData { sessionRepository.sendOTP(phone) }.showLoading().onSuccess {
            sendChangePasswordOtpResult.value = it
        }.execute()
    }

    val changeResult = MutableLiveData<LoginSessionResponse?>()
    fun changePassword(phone: String, code: String, password: String) {
        launchData {
            sessionRepository.changePassword(phone, code, password)
        }.showLoading().onSuccess {
            changeResult.value = it
        }.execute()
    }

    val setPwdResult = MutableLiveData<LoginSessionResponse?>()
    fun setPassword(phone: String, password: String) {
        launchData {
            sessionRepository.setPassword(phone, password)
        }.showLoading().onSuccess {
            recordEvent(
                TrackBean(
                    p = PageCreatePassword,
                    act = ACT_createPassword,
                    result = it.toJsonString()
                )
            )
            setPwdResult.value = it
        }.onFailed {
            recordEvent(
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
