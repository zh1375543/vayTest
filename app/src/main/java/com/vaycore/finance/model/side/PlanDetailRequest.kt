package com.vaycore.finance.model.side

import com.vaycore.finance.data.APPCODE

data class PlanDetailRequest(

    val appCode: String = APPCODE,
    val id: String? = null,
)
