package com.vaycore.finance.data.local.bean

data class ApiResponse<T>(
    var code: Int? = 0,
    var message: String? = null,
    var errorField: String? = null,
    var timestamp: Long? = 0L,
    val data: T? = null,
    var disabledToast: Boolean = false,
)