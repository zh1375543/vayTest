package com.vaycore.finance.model.identity

data class KycDocumentResponse(
    val id: Long,
    val userId: Long,
    val frontImageUrl: String? = null,
    val backImageUrl: String? = null,
    val liveImageUrl: String? = null,
    val idCardType: String? = null,
)
