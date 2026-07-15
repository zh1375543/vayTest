package com.vaycore.finance.data.local.bean

data class KycDocumentResponse(
    val id: Long,
    val userId: Long,
    val frontImageUrl: String? = null,
    val backImageUrl: String? = null,
    val liveImageUrl: String? = null,
)
