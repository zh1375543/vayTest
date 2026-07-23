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
    val planNo: String? = null,
    val planName: String? = null,
    val planIcon: String? = null,
    val targetAmount: BigDecimal? = null,
    val savedAmount: BigDecimal? = null,
    val remainingAmount: BigDecimal? = null,
    val currency: String? = null,
    val frequencyType: Int? = null,
    val frequencyText: String? = null,
    val eachAmount: BigDecimal? = null,
    val startDate: String? = null,
    val targetDate: String? = null,
    val nextSaveDate: String? = null,
    val status: Int? = null,
    val statusText: String? = null,
    val finishType: Int? = null,
    val finishTypeText: String? = null,
    val saveTimes: Int? = null,
    val finishTime: String? = null,
    val cancelTime: String? = null,
    val cancelRemark: String? = null,
    val progressRate: BigDecimal? = null,
    val progressPercent: BigDecimal? = null,
    val remainingDays: Int? = null,
    val remainingDaysText: String? = null,
    val nextSaveDateText: String? = null,
    val suggestSaveAmount: BigDecimal? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
    val extField1: String? = null,
    val extField2: String? = null,
    val extField3: String? = null,
    val extField4: String? = null,
    val extField5: String? = null,
)
