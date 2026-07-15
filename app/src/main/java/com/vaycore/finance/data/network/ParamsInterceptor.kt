package com.vaycore.finance.data.network

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject

class ParamsInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        val commonParams = mapOf(
            "appCode" to APPCODE,
            "version" to BuildConfig.VERSION_NAME,
            "mobileType" to "2"
        )

        return when (request.method.uppercase()) {

            "GET" -> handleGet(chain, request, commonParams)

//            "POST" -> handlePost(chain, request, commonParams)

            else -> chain.proceed(request)
        }
    }

    /**
     * GET
     */
    private fun handleGet(
        chain: Interceptor.Chain,
        request: Request,
        commonParams: Map<String, String>
    ): Response {

        val urlBuilder = request.url.newBuilder()

        val existsKeys = request.url.queryParameterNames

        commonParams.forEach { (key, value) ->
            if (!existsKeys.contains(key)) {
                urlBuilder.addQueryParameter(key, value)
            }
        }

        return chain.proceed(
            request.newBuilder()
                .url(urlBuilder.build())
                .build()
        )
    }

    /**
     * POST
     */
    private fun handlePost(
        chain: Interceptor.Chain,
        request: Request,
        commonParams: Map<String, String>
    ): Response {

        val body = request.body ?: return chain.proceed(request)

        return when (body) {

            is FormBody -> handleFormBody(chain, request, body, commonParams)

            else -> handleJsonBody(chain, request, body, commonParams)
        }
    }

    /**
     * FormBody
     */
    private fun handleFormBody(
        chain: Interceptor.Chain,
        request: Request,
        body: FormBody,
        commonParams: Map<String, String>
    ): Response {

        val builder = FormBody.Builder()

        val existsKeys = mutableSetOf<String>()

        for (i in 0 until body.size) {

            val key = body.name(i)
            val value = body.value(i)

            existsKeys.add(key)

            builder.add(key, value)
        }

        commonParams.forEach { (key, value) ->
            if (!existsKeys.contains(key)) {
                builder.add(key, value)
            }
        }

        return chain.proceed(
            request.newBuilder()
                .post(builder.build())
                .build()
        )
    }

    /**
     * JSON Body
     */
    private fun handleJsonBody(
        chain: Interceptor.Chain,
        request: Request,
        body: RequestBody,
        commonParams: Map<String, String>
    ): Response {

        try {

            val buffer = Buffer()
            body.writeTo(buffer)

            val charset = Charsets.UTF_8

            val bodyString = buffer.readString(charset)

            if (bodyString.isBlank()) {
                return chain.proceed(request)
            }

            val jsonObject = JSONObject(bodyString)

            commonParams.forEach { (key, value) ->

                if (!jsonObject.has(key)) {
                    jsonObject.put(key, value)
                }
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()

            val newBody = jsonObject.toString()
                .toRequestBody(mediaType)

            return chain.proceed(
                request.newBuilder()
                    .post(newBody)
                    .build()
            )

        } catch (e: Exception) {

            e.printStackTrace()

            return chain.proceed(request)
        }
    }
}