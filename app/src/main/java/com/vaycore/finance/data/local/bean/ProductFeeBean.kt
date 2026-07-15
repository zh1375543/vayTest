package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ProductFeeBean(
    val productId: Long,
    val name: String? = null,
    val nameConfig: String? = null,
    val amount: BigDecimal? = null,
) : Parcelable {
    fun getFeeName(): String? = name
}
