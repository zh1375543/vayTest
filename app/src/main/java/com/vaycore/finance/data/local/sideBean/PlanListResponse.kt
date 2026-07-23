package com.vaycore.finance.data.local.sideBean

data class PlanListResponse(
    val total: Int? = null,
    val list: List<PlanItem>? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
    val pages: Int? = null,
)
