package com.vaycore.finance.identity

import android.os.Build
import com.appsflyer.AppsFlyerLib
import com.vaycore.finance.app.App
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.appFlyer
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.firebaseId
import com.vaycore.finance.data.firebaseToken
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.data.location
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.model.identity.LoginSessionResponse
import com.vaycore.finance.util.runtime.DeviceHelper
import com.vaycore.finance.util.toMd5

class SessionRepository(
    private val api: Api,
) {

    suspend fun sendOTP(phone: String): Any? {
        return api.sendSMS(ApiRequest(phone = phone)).dataOrThrow()
    }

    suspend fun login(
        phone: String,
        code: String?,
        password: String?,
        coordinate: Pair<Double, Double> = location,
    ): LoginSessionResponse? {
        val param = ApiRequest(
            phone = phone,
            coordinate = "${coordinate.first},${coordinate.second}",
            regClient = "Android",
            smsCode = code,
            appsflyerId = AppsFlyerLib.getInstance()
                .getAppsFlyerUID(App.Companion.appContext)
                ?: "",
            content = appFlyer,
            phoneMark = DeviceHelper.getDeviceId(),
            passwd = password?.toMd5(),
            loginType = if (code != null) 1 else 2,
            firebaseClientId = firebaseId,
            firebaseToken = firebaseToken,
        )
        return api.login(param).dataOrThrow()
    }

    suspend fun logout(): Any? {
        return api.logout(ApiRequest(rid = loginInfo?.id)).dataOrThrow()
    }

    suspend fun setPassword(phone: String, password: String): LoginSessionResponse? {
        return api.fetchPassword(
            ApiRequest(phone = phone, newPasswd = password.toMd5())
        ).dataOrThrow()
    }

    suspend fun changePassword(phone: String, code: String, password: String): LoginSessionResponse? {
        return api.updatePassword(
            ApiRequest(phone = phone, smsCode = code, newPasswd = password.toMd5())
        ).dataOrThrow()
    }

    suspend fun postDeviceInfo(): Any? {
        if (!isLogin) return null
        return api.postDeviceInfo(
            ApiRequest(
                phoneModel = Build.MODEL,
                phoneBrand = Build.BRAND,
                phoneMark = DeviceHelper.getDeviceId(),
                appVersion = BuildConfig.VERSION_NAME,
                regClient = "Android"
            )
        ).dataOrThrow()
    }

}