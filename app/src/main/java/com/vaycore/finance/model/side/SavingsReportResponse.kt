package com.vaycore.finance.model.side

import java.math.BigDecimal

data class SavingsReportResponse(
    val totalSavedAmount: BigDecimal? = null,
    val monthSavedAmount: BigDecimal? = null,
    val savingDays: Int? = null,
    val totalPlanCount: Int? = null,
    val processingPlanCount: Int? = null,
    val finishedPlanCount: Int? = null,
    val averageSaveAmount: BigDecimal? = null,
    val saveRecordCount: Int? = null,
    val levelCode: String? = null,
    val levelName: String? = null,
    val levelText: String? = null,
)