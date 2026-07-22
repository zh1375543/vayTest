package com.vaycore.finance.data.local.sideBean

import java.math.BigDecimal

data class PlanCalendarResponse(
    val year: Int? = null,
    val month: Int? = null,
    val weekStart: Int? = null,
    val days: List<CalendarDay>? = null,
)

data class CalendarDay(
    val date: String? = null,
    val day: Int? = null,
    val inCurrentMonth: Boolean? = null,
    val saveAmount: BigDecimal? = null,
    val withdrawAmount: BigDecimal? = null,
    val netAmount: BigDecimal? = null,
    val hasRecord: Boolean? = null,
)
