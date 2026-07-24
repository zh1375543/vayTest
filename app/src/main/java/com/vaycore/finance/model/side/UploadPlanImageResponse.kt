package com.vaycore.finance.model.side

data class UploadPlanImageResponse(
    val host: String? = null,
    val path: String? = null,
) {
    val imageUrl: String
        get() = host.orEmpty() + path.orEmpty()
}
