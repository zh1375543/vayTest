package com.vaycore.finance.data.local.bean

import java.math.BigDecimal

data class LoanDashboardResponse(
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    val calmFlag: Boolean = false,
    val enableLoanStr: String? = null,
    val loanAmountRange: String? = null,
    val bankErrorFlag: Boolean,
    val showMultipleRepaySign: Int = 0,
    val recommendText: String?=null,
    val showProducts: List<ProductBean>? = null,
    val repayProducts: List<ProductBean>? = null,
    val canNotApplyProducts: List<ProductBean>? = null,
    val userCreditAmount: BigDecimal? = null,
    val userCreditCurrency: String? = null,
    val userCreditCurrencySymbol: String? = null,
    val togetherLoanSign: Int? = null,
    val canApplyAmount: BigDecimal? = null,
    val allAmount: BigDecimal? = null,
    val bankInfoId: Long? = null,
    val bankNo: String? = null,
    val currency: String? = null,
    val currencySymbol: String? = null,
    val isNew: Int = 1,
    val totalCreditAmount: BigDecimal? = null,
    val usedAmount: BigDecimal? = null,
    val userCreditStatus: Int? = null,
)
