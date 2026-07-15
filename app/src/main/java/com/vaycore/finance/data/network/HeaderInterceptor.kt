package com.vaycore.finance.data.network

import com.vaycore.finance.data.local.appCheckToken
import com.vaycore.finance.data.local.token
import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request().newBuilder()
            .header("Content-Type", "application/json")
            .header("Content-Encoding", "gzip")
            .header("User-Agent", "Android")
            .header("lang", "en_US")
        if (token.isNotBlank()) {
            originalRequest.addHeader("Authorization", token)
        }
        if (appCheckToken.isNotBlank()) {
            originalRequest.header("X-Firebase-AppCheck", appCheckToken)
        }
        return chain.proceed(originalRequest.build())
    }
}
