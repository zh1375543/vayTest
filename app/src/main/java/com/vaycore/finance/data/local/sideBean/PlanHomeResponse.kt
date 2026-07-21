package com.vaycore.finance.data.local.sideBean

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PlanHomeResponse(
    val hasPlan: Boolean? = null,
    val totalSavedAmount: BigDecimal? = null,
    val monthSavedAmount: BigDecimal? = null,
    val planList: List<PlanItem>? = null,
)

data class PlanItem(
    val id: Int? = null,
    val planName: String? = null,
    val targetAmount: BigDecimal? = null,
    val savedAmount: BigDecimal? = null,
    val remainingAmount: BigDecimal? = null,
    val progressPercent: BigDecimal? = null,
    val nextSaveDateText: String? = null,
    val suggestSaveAmount: BigDecimal? = null,
    @SerializedName(value = "planIcon", alternate = ["planIconUrl", "iconUrl"])
    val planIcon: String? = null,
    @SerializedName(value = "remainingDays", alternate = ["remainDays", "daysLeft"])
    val remainingDays: Int? = null,
)
