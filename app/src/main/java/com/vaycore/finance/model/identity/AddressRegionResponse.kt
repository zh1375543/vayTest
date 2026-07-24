package com.vaycore.finance.model.identity

data class AddressRegionResponse(
    val id: Int,
    val parentId: Long,
    val name: String? = null,
    val otherName: String? = null,
    val type: Int,
    val countryId: Int,
)