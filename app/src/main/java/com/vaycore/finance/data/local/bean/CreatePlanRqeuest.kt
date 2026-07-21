package com.vaycore.finance.data.local.bean

import com.vaycore.finance.data.local.APPCODE

data class CreatePlanRequest(
    val planName: String? = null,
    val eachAmount: Int? = null,
    val frequencyType: Int? = null,
    val targetAmount: Int? = null,
    val planIcon: String? = null,
    val appCode: String = APPCODE,

)
