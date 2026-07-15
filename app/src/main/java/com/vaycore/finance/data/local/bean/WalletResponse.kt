package com.vaycore.finance.data.local.bean

data class WalletResponse(
    val id: Int,
    val walletCode: String?,
    val walletName: String?,
    val walletType: String?,
    val walletKey: String?,
    val walletValue: String?,
    val walletDesc: String?,
    val status: Int?,
    val sort: Int?,
    var defaultSign: Int? = null,
    val effectiveTime: String?,
    val expireTime: String?,
    val operator: String?,
    val createTime: String?,
    val updateTime: String?,
    val accountCode: String?
)
