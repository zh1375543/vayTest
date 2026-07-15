package com.vaycore.finance.util

import android.os.Bundle
import com.appsflyer.AppsFlyerLib
import com.google.firebase.analytics.FirebaseAnalytics
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.App

const val LOGIN_VIA_OTP = "Login_Via_OTP" // on login button click
const val APP_UPGRADE = "App_Upgrade" // on version update button click
const val PERSON_INFO_COMMIT = "Person_Info_Commit" // on personal info submit
const val SUPPLEMENTARY_INFO_COMMIT = "Supplementary_Info_Commit" // on contact info submit
const val KYC_INFO_COMMIT = "Kyc_Info_Commit" // on KYC submit
const val ORDER_COMMIT = "Order_Commit" // on order submit button click
const val PROFILE_PAGE = "Profile" // visit auth main page
const val PERSON_INFO_PAGE = "person_info" // visit personal info page
const val KYC_INFO_PAGE = "kyc_info" // visit KYC page
const val SUPPLEMENTARY_INFO_PAGE = "supplementary_info" // visit work info page
const val WALLET_INFO_PAGE = "wallet_info" // visit wallet/bank card page
const val KYC_AADHAAR_FRONT_CLICK = "kyc_aadhaar_front" // on ID card front click
const val KYC_AADHAAR_BACK_CLICK = "kyc_aadhaar_back" // on ID card back click
const val WORK_OCCUPATION_CLICK = "work_occupation" // on contact Occupation selection
const val LOAN_GET_NOW_CLICK = "loan_get_now" // on product Apply click
const val LOAN_ORDER_CONFIRMATION_PAGE = "loan_order_confirmation" // visit order submit page
const val FK_FAIL = "fk_fail" // risk control failure callback

fun trackEvent(event: String) {
    trackEvent(event, emptyMap())
}

fun trackEvent(event: String, params: Map<String, Any?>) {
    if (BuildConfig.DEBUG ) return

    val appsFlyerParams = params.mapNotNull { (key, value) ->
        value?.let { key to it }
    }.toMap()
    val appsFlyerPayload = appsFlyerParams.takeIf { it.isNotEmpty() }

    runCatching {
        AppsFlyerLib.getInstance().logEvent(App.appContext, event, appsFlyerPayload)
    }.onFailure {
        LogUtil.e("AppsFlyer track failed: ${it.message}")
    }

    runCatching {
        FirebaseAnalytics.getInstance(App.appContext).logEvent(event, params.toFirebaseBundle())
    }.onFailure {
        LogUtil.e("Firebase track failed: ${it.message}")
    }
}

private fun Map<String, Any?>.toFirebaseBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        val safeKey = key.toFirebaseParamName()
        when (value) {
            null -> return@forEach
            is String -> putString(safeKey, value)
            is Int -> putInt(safeKey, value)
            is Long -> putLong(safeKey, value)
            is Double -> putDouble(safeKey, value)
            is Float -> putFloat(safeKey, value)
            is Boolean -> putBoolean(safeKey, value)
            else -> putString(safeKey, value.toString())
        }
    }
}

private fun String.toFirebaseParamName(): String {
    val normalized = replace(Regex("[^A-Za-z0-9_]"), "_").take(40)
    return if (normalized.firstOrNull()?.isLetter() == true) normalized else "p_$normalized"
}
