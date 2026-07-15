package com.vaycore.finance.data.local.bean

data class AddressRegionResponse(
    val id: Int,
    val parentId: Long,
    val name: String? = null,
    val otherName: String? = null,
    val type: Int,
    val countryId: Int,
)
