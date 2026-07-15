package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class OrderBean(
    val id: Long? = null,
    val userId: Long? = null,
    val orderNo: String? = null,
    val historyOrders: Int? = null,
    val productId: Long? = null,
    val loanAmount: BigDecimal? = null,
    val productName: String? = null,
    val closeReason: String? = null,
    val closeRemark: String? = null,
    val closeTime: String? = null,
    val statusStr: String? = null,
    val orderId: Long? = null,
    val currency: String? = null,
    val currencySymbol: String? = null,
    val timeLimit: Int? = null,
    val status: Int? = null,
    val cardNo: String? = null,
    val createTime: String? = null,
    val bankNo: String? = null,
    val orderHandleFees: List<ProductFeeBean>? = null,
    val actualNeedRepayAmount: BigDecimal? = null,
    val penaltyAmount: BigDecimal? = null,
    val payGoUrl: String? = null,
) : Parcelable
