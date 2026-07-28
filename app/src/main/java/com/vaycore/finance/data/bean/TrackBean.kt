package com.vaycore.finance.data.bean

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.app.App
import com.vaycore.finance.data.afSource
import com.vaycore.finance.data.gaId
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.data.refer
import com.vaycore.finance.util.deivce.DeviceIdentityReader
import com.vaycore.finance.util.removeWhitespace

data class TrackParamBean(
    val __logs__: List<SurveyBean>,
    val __topic__: String = "survey",
)

data class SurveyBean(
    val survey: String,
)

data class TrackBean(
    val env: String = if (BuildConfig.HTTP_HOST.contains("api.vaynflash.com")) "prod" else "staging",
    val v: Int = 1,
    val t: String = System.currentTimeMillis().toString(),
    val m: String? = loginInfo?.phone,
    val p: String? = null,
    var pp: String? = null,
    var prevAct: String? = null,
    var prevP: String? = null,
    var lastAct: String? = null,
    var lastP: String? = null,
    val deviceId: String? = gaId.ifBlank { DeviceIdentityReader.getDeviceId() },
    val source: String? = afSource,
    val referer: String? = refer,
    val vestName: String? = App.appContext.resources.getString(R.string.app_name).removeWhitespace(),
    val type: String = "app",
    val act: String? = null,
    val code: String = "discarded",
    val result: String? = null,
)
