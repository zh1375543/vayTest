package com.vaycore.finance.data.local.sideBean

import com.vaycore.finance.data.local.APPCODE

data class PlanListRequest(
    val appCode: String = APPCODE,
    val status: Int? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
)