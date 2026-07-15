package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class LoanTermBean(
    val id: Long? = null,
    val name: String? = null,
    val status: Int? = null,
    val timeLimit: Int? = null,
    val interestRate: Double? = null,
    val interestRateType: Int? = null,
    val handleFeeState: Int? = null,
    val defaultSign: Int? = null,
    val actualAmount: BigDecimal? = null,
    val serviceAmount: BigDecimal? = null,
    val afterHandleAmount: BigDecimal? = null,
    val interestAmount: BigDecimal? = null,
    val dailyAmount: BigDecimal? = null,
    val actualRepayAmount: BigDecimal? = null,
    val repayTimeStr: String? = null,
    val applySign: Int? = null,
) : Parcelable
