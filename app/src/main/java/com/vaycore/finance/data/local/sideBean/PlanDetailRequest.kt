package com.vaycore.finance.data.local.sideBean

import com.vaycore.finance.data.local.APPCODE

data class PlanDetailRequest(

    val appCode: String = APPCODE,
    val id: String? = null,
)
