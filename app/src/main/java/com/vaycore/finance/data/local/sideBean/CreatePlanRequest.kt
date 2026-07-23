package com.vaycore.finance.data.local.sideBean

import com.vaycore.finance.data.local.APPCODE

data class CreatePlanRequest(
    val planName: String? = null,
    val eachAmount: Int? = null,
    val frequencyType: Int? = null,
    val targetAmount: Int? = null,
    val planIcon: String? = null,
    val appCode: String = APPCODE,

)

data class UpdatePlanRequest(
    val id: Long? = null,
    val planName: String? = null,
    val planIcon: String? = null,
    val appCode: String = APPCODE,
    val extField1: String? = null,
    val extField2: String? = null,
    val extField3: String? = null,
    val extField4: String? = null,
    val extField5: String? = null,
)

data class CancelPlanRequest(
    val id: Long? = null,
    val appCode: String = APPCODE,
)
