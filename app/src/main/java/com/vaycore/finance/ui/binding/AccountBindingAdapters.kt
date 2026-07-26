package com.vaycore.finance.ui.binding

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.vaycore.finance.util.maskSensitive
import com.vaycore.finance.util.formatAmountWithPrefix
import java.math.BigDecimal

@BindingAdapter("accountType")
fun TextView.bindAccountType(payWay: String?) {
    text = payWay?.lowercase().orEmpty()
}

@BindingAdapter("maskedAccount")
fun TextView.bindMaskedAccount(account: String?) {
    text = account.maskSensitive().orEmpty()
}

@BindingAdapter(value = ["amountWithCurrency", "amountCurrencySymbol"])
fun TextView.bindAmountWithCurrency(amount: BigDecimal?, currencySymbol: String?) {
    text = amount.formatAmountWithPrefix(currencySymbol)
}
