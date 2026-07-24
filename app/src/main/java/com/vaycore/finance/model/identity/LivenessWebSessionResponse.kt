package com.vaycore.finance.model.identity

data class LivenessWebSessionResponse(
    val verifyUrl: String? = null,
    val bizNo: String? = null,
    val expiredTime: Long? = null,
    val faceUrl: String? = null,
)