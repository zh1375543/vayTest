package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ProductPlanBean(
    val planPart: Int? = null,
    val planPartStr: String? = null,
    val timeLimit: Int? = null,
    val repayActualAmountRate: BigDecimal? = null,
    val repayAfterHandleAmountRate: BigDecimal? = null,
    val repayServiceFee: BigDecimal? = null,
    val repayInterestAmountRate: BigDecimal? = null,
    val repayActualAmount: BigDecimal? = null,
    val repayAfterHandleAmount: BigDecimal? = null,
    val repayInterestAmount: BigDecimal? = null,
    val totalRepayment: BigDecimal? = null,
    val repayTime: String? = null,
    val id: Long? = null,
    val orderNo: String? = null,
    val installmentOrderNo: String? = null,
    val loanAmount: BigDecimal? = null,
    val needRepayLoanAmount: BigDecimal? = null,
    val needRepayInterestSum: BigDecimal? = null,
    val interestSum: BigDecimal? = null,
    val actualNeedRepayAmount: BigDecimal? = null,
    val afterHandleAmount: BigDecimal? = null,
    var planStatus: Int? = null,
    val planStatusStr: Int? = null,
    val needRepayPenaltyAmount: BigDecimal? = null,
    val needRepayAfterHandleAmount: BigDecimal? = null,
    var isSelect: Boolean = false,
    var isExpend: Boolean = false,
) : Parcelable {
    fun isDueAndSettle(): Boolean {
        return when (planStatus) {
            34, 35, 40, 41, 42, 43 -> true
            else -> false
        }
    }

    fun isDue(): Boolean {
        return planStatus == 34 || planStatus == 35
    }

    fun isSettle(): Boolean {
        return when (planStatus) {
            40, 41, 42, 43 -> true
            else -> false
        }
    }

    fun isProcess(): Boolean {
        return planStatus == 31
    }
}
