package com.vaycore.finance.model.side

import com.vaycore.finance.data.APPCODE

data class PlanListRequest(
    val appCode: String = APPCODE,
    val status: Int? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
)