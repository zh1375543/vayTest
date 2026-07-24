package com.vaycore.finance.model.side

import com.vaycore.finance.data.APPCODE

data class PlanCalendarRequest(
    val appCode: String = APPCODE,
    val planId: Long? = null,
    val year: Int? = null,
    val month: Int? = null,
    val timezone: String? = null,
)
