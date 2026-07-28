package com.vaycore.finance.data.network

import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.toJsonString
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer

/** Extracts the final request payload used by the signing protocol. */
class RequestPayloadExtractor {

    fun extract(request: Request): String {
        val body = request.body
        return when {
            body?.contentType()?.subtype == "json" -> readBody(body)
            body is FormBody -> extractFormPayload(request, body)
            body is MultipartBody -> extractMultipartPayload(request, body)
            else -> ""
        }
    }

    private fun extractFormPayload(request: Request, formBody: FormBody): String {
        val params = mutableMapOf<String, Any?>()
        for (index in 0 until formBody.size) {
            params[formBody.name(index)] = formBody.value(index)
        }
        appendQueryParameters(request, params)
        return params.toJsonString()
    }

    private fun extractMultipartPayload(request: Request, multipartBody: MultipartBody): String {
        val params = mutableMapOf<String, Any?>()
        multipartBody.parts.forEach { part ->
            val contentDisposition = part.headers?.get("Content-Disposition") ?: return@forEach
            if (!contentDisposition.contains("form-data; name=")) return@forEach

            val name = contentDisposition.substringAfter("name=\"").substringBefore("\"")
            if (contentDisposition.contains("filename=\"")) return@forEach

            val value = readBody(part.body)
            if (value.isNotBlank() && name != "eventFile") {
                params[name] = value
            }
        }
        appendQueryParameters(request, params)
        return params.toJsonString()
    }

    private fun appendQueryParameters(request: Request, params: MutableMap<String, Any?>) {
        request.url.queryParameterNames.forEach { name ->
            params[name] = request.url.queryParameter(name)
        }
    }

    private fun readBody(body: RequestBody): String {
        if (body.isOneShot() || body.isDuplex()) {
            LogUtil.e("Skip signing payload extraction for a one-shot or duplex request body")
            return ""
        }
        return try {
            Buffer().use { buffer ->
                body.writeTo(buffer)
                buffer.readUtf8()
            }
        } catch (exception: Exception) {
            LogUtil.e("Read request body for signing failed: ${exception.message}")
            ""
        }
    }
}
