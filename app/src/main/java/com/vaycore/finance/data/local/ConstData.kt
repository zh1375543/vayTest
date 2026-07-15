package com.vaycore.finance.data.local

import com.vaycore.finance.data.local.bean.LoginSessionResponse
import com.vaycore.finance.util.SPUtil
import com.vaycore.finance.util.parseJsonList
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.parseJson

const val APPCODE = "gaanpuhunan"
var EnterTime: String = System.currentTimeMillis().toString()
var pCount = 0
var HomeLoanAmountRange: String? = null

var appFlyer: String
    get() = SPUtil.newInstance().get("AF_KEY", "")
    set(value) {
        SPUtil.newInstance().save("AF_KEY", value)
    }

var language: String
    get() = "en"
    set(value) {
        SPUtil.newInstance().save("LANGUAGE_KEY", "en")
    }

var token: String
    get() = SPUtil.getInstance().get("TOKEN_KEY", "")
    set(value) {
        SPUtil.getInstance().save("TOKEN_KEY", value)
    }
val isLogin: Boolean
    get() = token.isNotBlank()

var agreePrivacy: Boolean
    get() = SPUtil.newInstance().get("AGREE_PRIVACY_KEY", false)
    set(value) {
        SPUtil.newInstance().save("AGREE_PRIVACY_KEY", value)
    }

var agreePhonePrivacy: Boolean
    get() = SPUtil.newInstance().get("AGREE_PHONE_KEY", false)
    set(value) {
        SPUtil.newInstance().save("AGREE_PHONE_KEY", value)
    }

var location: Pair<Double, Double>
    get() = SPUtil.getInstance()
        .get("LOCATION_KEY", "")
        .parseJson<Pair<Double, Double>>() ?: (0.0 to 0.0)
    set(value) {
        SPUtil.getInstance().save("LOCATION_KEY", value.toJsonString())
    }

var loginInfo: LoginSessionResponse?
    get() = SPUtil.getInstance()
        .get("LOGIN_KEY", "")
        .parseJson<LoginSessionResponse?>()
    set(value) {
        SPUtil.getInstance().save("LOGIN_KEY", value?.toJsonString() ?: "")
    }

var signBackHome: Boolean
    get() = SPUtil.getInstance().get("FIRST_SIGN_BACK_HOME_KEY", false)
    set(value) {
        SPUtil.getInstance().save("FIRST_SIGN_BACK_HOME_KEY", value)
    }

var st: String
    get() = SPUtil.newInstance().get("ST_KEY", "")
    set(value) {
        SPUtil.newInstance().save("ST_KEY", value)
    }

var authConfigList: List<String>
    get() = SPUtil.getInstance()
        .get("CONFIG_AUTH_LIST_KEY", "")
        .parseJsonList<String>() ?: emptyList()
    set(value) {
        SPUtil.getInstance().save("CONFIG_AUTH_LIST_KEY", value.toJsonString())
    }

var isPostDeviceInfo: Boolean
    get() = SPUtil.getInstance().get("USER_DEVICE_KEY", false)
    set(value) {
        SPUtil.getInstance().save("USER_DEVICE_KEY", value)
    }

var refer: String
    get() = SPUtil.newInstance().get("GAREFER_KEY", "")
    set(value) {
        SPUtil.newInstance().save("GAREFER_KEY", value)
    }

var afSource: String
    get() = SPUtil.newInstance().get("AF_SOURCE_KEY", "")
    set(value) {
        SPUtil.newInstance().save("AF_SOURCE_KEY", value)
    }

var gaId: String
    get() = SPUtil.newInstance().get("GAID_KEY", "")
    set(value) {
        SPUtil.newInstance().save("GAID_KEY", value)
    }

var appCheckToken: String
    get() = SPUtil.newInstance().get("APP_CHECK_TOKEN_KEY", "")
    set(value) {
        SPUtil.newInstance().save("APP_CHECK_TOKEN_KEY", value)
    }

var rateApp: Boolean
    get() = SPUtil.newInstance().get("RATE_APP_KEY", false)
    set(value) {
        SPUtil.newInstance().save("RATE_APP_KEY", value)
    }

var firebaseToken: String
    get() = SPUtil.newInstance().get("FIREBASE_TOKEN_KEY", "")
    set(value) {
        SPUtil.newInstance().save("FIREBASE_TOKEN_KEY", value)
    }

var firebaseId: String
    get() = SPUtil.newInstance().get("FIREBASE_ID_KEY", "")
    set(value) {
        SPUtil.newInstance().save("FIREBASE_ID_KEY", value)
    }

const val ORDER_STATUS_NOT = -1
const val ORDER_STATUS_SUCCESS = 10
const val ORDER_STATUS_REVIEW = 11 // pre-review
const val ORDER_STATUS_AUTO = 12 // auto review
const val ORDER_STATUS_MANUAL = 13 // manual review
const val ORDER_STATUS_AUTO_FAIL = 14
const val ORDER_STATUS_MANUAL_FAIL = 15
const val ORDER_STATUS_AUTO_SUCCESS = 16
const val ORDER_STATUS_MANUAL_SUCCESS = 17
const val ORDER_STATUS_BANK_VERIFIED = 18
const val ORDER_STATUS_SIGNED = 20
const val ORDER_STATUS_CASH = 21
const val ORDER_STATUS_INVALID = 22
const val ORDER_STATUS_CLOSE = 23
const val ORDER_STATUS_PAYMENT_ING = 24
const val ORDER_STATUS_PAYMENT_FAIL = 25
const val ORDER_STATUS_PAYMENT_PENDING = 30 // pending repayment
const val ORDER_STATUS_PAYMENT_PROCESS = 31 // repayment processing
const val ORDER_STATUS_IN_RENEWAL = 32
const val ORDER_STATUS_IN_RENEWAL_PROCESS = 33
const val ORDER_STATUS_OVERDUE = 34 // overdue
const val ORDER_STATUS_BAD_DEBTS = 35
const val ORDER_STATUS_SETTLE = 40
const val ORDER_STATUS_SETTLE_REDUCE = 41
const val ORDER_STATUS_SETTLE_RENEWAL = 42
const val ORDER_STATUS_SETTLE_REDUCE_OR_RENEWAL = 43


const val AF_KEY = "XyR2FTp7tA3UDGzmzNqtui"

private const val baseWebUrl = "https://www.papavay.com"
const val PRIVACY_POLICY = "$baseWebUrl/agreement/protocol_privacy_index.html"
const val AGREEMENT_ABOUT = "$baseWebUrl/agreement/about.html"
const val AGREEMENT_REGISTER = "$baseWebUrl/agreement/register.html"

const val HTTP_INFORMATION_COLLECTION =
    "$baseWebUrl/agreement/contact_license_agreement.html"
const val PRIVACY_COLLECT =
    "$baseWebUrl/agreement/Information_collection_service_agreement.html"
const val LEASE_AGREEMENT = "$baseWebUrl/agreement/leaseAgreement.html?"
const val PAWN_AGREEMENT = "$baseWebUrl/agreement/pawnAgreement.html?"
