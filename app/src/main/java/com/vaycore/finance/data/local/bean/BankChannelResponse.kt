package com.vaycore.finance.data.local.bean

data class BankChannelResponse(
    val id: Int,
    var status: Int = 0,
    var bankCode: String? = null,
    var bankName: String? = null,
    var longCode: String? = null,
    var logoUrl: String? = null,
    var isSelect: Boolean = false,
    var countryId: Long? = null,
)
