package com.vaycore.finance.util

import java.math.BigDecimal
import java.text.DecimalFormat

fun BigDecimal?.formatAmount(symbol: String? = "₫"): String {
    val formatter = DecimalFormat("#,###.##")  // up to 2 decimal places
    return formatter.format(this ?: 0) + (symbol ?: "")
}

fun BigDecimal?.formatAmountWithPrefix(symbol: String? = "₫"): String {
    val formatter = DecimalFormat("#,###.##")
    return (symbol ?: "") + formatter.format(this ?: 0)
}
