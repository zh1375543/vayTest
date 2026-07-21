package com.vaycore.finance.data.local.bean

import java.math.BigDecimal

data class GuestHomeResponse(
    val maxAmount: BigDecimal? = null,
    val annualizedInterestRate: String? = null,
    val loanTerm: String? = null,
    val customerEmail: String? = null,
    val customerPhone: String? = null,
    val currencySymbol: String? = null,
    val currency: String? = null,
    val appApplyJumpPage: String? = null,
    val recommendText: String? = null,
    val showBackButton: String? = null,
    val customerConfigs: List<CustomerContactConfig>? = null,
)

data class CustomerContactConfig(
    val enTitle: String? = null,
    val vernacularTitle: String? = null,
    val content: String? = null,
    val buttonType: Int? = null,
)
