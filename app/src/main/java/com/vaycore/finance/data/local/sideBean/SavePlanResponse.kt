package com.vaycore.finance.data.local.sideBean

import java.math.BigDecimal

data class SavePlanResponse(
    val id: Long? = null,
    val recordNo: String? = null,
    val planId: Long? = null,
    val planName: String? = null,
    val recordType: Int? = null,
    val recordTypeText: String? = null,
    val amount: BigDecimal? = null,
    val beforeAmount: BigDecimal? = null,
    val afterAmount: BigDecimal? = null,
    val currency: String? = null,
    val occurTime: String? = null,
    val remark: String? = null,
    val imageUrls: String? = null,
    val imageCount: Int? = null,
    val locationText: String? = null,
)