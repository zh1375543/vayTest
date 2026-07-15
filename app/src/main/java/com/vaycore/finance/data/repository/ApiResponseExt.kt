package com.vaycore.finance.data.repository

import com.vaycore.finance.data.local.bean.ApiResponse

class ApiResponseException(
    val response: ApiResponse<*>,
) : RuntimeException(response.message)

fun <T> ApiResponse<T?>.dataOrThrow(): T? {
    if (code == 200) return data
    throw ApiResponseException(this)
}
