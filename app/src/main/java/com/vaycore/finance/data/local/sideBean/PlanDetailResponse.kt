package com.vaycore.finance.data.local.sideBean

import java.math.BigDecimal

data class PlanDetailResponse(
    val plan: PlanDetail? = null,
    val recordList: List<PlanRecord>? = null,
)

data class PlanDetail(
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

data class PlanRecord(
    val id: Int? = null,
    val recordType: Int? = null,
    val recordTypeText: String? = null,
    val amount: BigDecimal? = null,
    val beforeAmount: BigDecimal? = null,
    val afterAmount: BigDecimal? = null,
    val imageCount: Int? = null,
    val occurTime: String? = null,
    val locationText: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val remark: String? = null,
    val imageUrls: String? = null,
)
