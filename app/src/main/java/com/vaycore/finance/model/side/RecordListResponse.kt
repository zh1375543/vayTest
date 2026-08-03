package com.vaycore.finance.model.side

import java.math.BigDecimal

data class RecordListResponse(
    val total: Int? = null,
    val list: List<RecordItem>? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null,
)

data class RecordItem(
    val id: Int? = null,
    val planId: Int? = null,
    val planName: String? = null,
    val recordType: Int? = null,
    val recordTypeText: String? = null,
    val amount: BigDecimal? = null,
    val beforeAmount: BigDecimal? = null,
    val afterAmount: BigDecimal? = null,
    val imageCount: Int? = null,
    val occurTime: String? = null,
)