package com.vaycore.finance.data.network

import com.vaycore.finance.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class TrackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url.toString()

        if (requestUrl.contains(BuildConfig.TRACK_HOST)) {
            val body = request.body
            val build = request.newBuilder()
                .addHeader("x-log-apiversion","0.6.0")
                .addHeader(
                    "x-log-bodyrawsize",
                    body?.contentLength()?.toString() ?: "0"
                )
                .addHeader("Connection", "keep-alive")
                .build()
            return chain.proceed(build)
        }
        return chain.proceed(request)
    }
}