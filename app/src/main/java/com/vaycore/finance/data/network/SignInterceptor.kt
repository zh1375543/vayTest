package com.vaycore.finance.data.network

import com.vaycore.finance.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/** Adds signing headers after request parameters have been finalized. */
class SignInterceptor(
    private val payloadExtractor: RequestPayloadExtractor = RequestPayloadExtractor(),
    private val signatureGenerator: SignatureGenerator = SignatureGenerator()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.toString().contains(BuildConfig.TRACK_HOST)) {
            return chain.proceed(request)
        }

        val headers = signatureGenerator.generate(payloadExtractor.extract(request))
        val signedRequest = request.newBuilder()
            .addHeader("sign", headers.sign)
            .addHeader("timestamp", headers.timestamp)
            .build()
        return chain.proceed(signedRequest)
    }
}
