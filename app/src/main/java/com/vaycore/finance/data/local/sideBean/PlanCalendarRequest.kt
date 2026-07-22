package com.vaycore.finance.data.local.sideBean

import com.vaycore.finance.data.local.APPCODE

data class PlanCalendarRequest(
    val appCode: String = APPCODE,
    val planId: Long? = null,
    val year: Int? = null,
    val month: Int? = null,
    val timezone: String? = null,
)
