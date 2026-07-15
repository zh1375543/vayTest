package com.vaycore.finance.data.local.bean

// KYC_BACK: 0=not required, 1=upload, 2=upload+OCR
// FACE_COMPARE: 0=not required, 1=required
// KYC_FRONT: 0=not required, 1=upload, 2=upload+OCR
// FACE: 0=not required, 1=photo (selfie), 2=liveness
data class KycRuleConfigResponse(
    val KYC_BACK: Int,
    val FACE_COMPARE: Int,
    val KYC_FRONT: Int,
    val FACE: Int,
)
