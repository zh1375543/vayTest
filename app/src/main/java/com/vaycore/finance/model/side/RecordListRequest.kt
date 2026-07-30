package com.vaycore.finance.model.side

import com.vaycore.finance.data.APPCODE

data class RecordListRequest (
    val appCode: String = APPCODE,
    val planId: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)
