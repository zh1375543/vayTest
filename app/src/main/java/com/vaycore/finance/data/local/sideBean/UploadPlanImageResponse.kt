package com.vaycore.finance.data.local.sideBean

data class UploadPlanImageResponse(
    val host: String? = null,
    val path: String? = null,
) {
    val imageUrl: String
        get() = host.orEmpty() + path.orEmpty()
}
