package com.vaycore.finance.ui.binding

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.vaycore.finance.R
import com.vaycore.finance.util.maskSensitive
import com.vaycore.finance.util.formatAmountWithPrefix
import java.math.BigDecimal

@BindingAdapter("accountType")
fun TextView.bindAccountType(payWay: String?) {
    text = when (payWay) {
        "CARD" -> context.getString(R.string.bank)
        "WALLET" -> context.getString(R.string.e_wallet)
        else -> ""
    }
}

/** Renders the local icon that matches the selected payout account type. */
@BindingAdapter("accountIcon")
fun ImageView.bindAccountIcon(payWay: String?) {
    setImageResource(
        if (payWay == "WALLET") R.mipmap.ic_wallet_defalut else R.mipmap.ic_bank_default,
    )
}

@BindingAdapter("maskedAccount")
fun TextView.bindMaskedAccount(account: String?) {
    text = account.maskSensitive().orEmpty()
}

@BindingAdapter(value = ["amountWithCurrency", "amountCurrencySymbol"])
fun TextView.bindAmountWithCurrency(amount: BigDecimal?, currencySymbol: String?) {
    text = amount.formatAmountWithPrefix(currencySymbol)
}
