package com.vaycore.finance.data.network

import com.vaycore.finance.util.isNetworkAvailable
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isNetworkAvailable()) {
            throw NoNetworkException("No internet connection")
        }

        return chain.proceed(chain.request())
    }
}

class NoNetworkException(message: String) : IOException(message)