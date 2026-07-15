package com.vaycore.finance.data.network

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.st
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.toMd5
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.util.SortedMap
import java.util.TreeMap
import kotlin.collections.set

class SignInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url.toString()

        if (requestUrl.contains(BuildConfig.TRACK_HOST)) {
            return chain.proceed(request)
        }
        val contentType = request.body?.contentType()?.subtype

        val bodyJson = when {
            contentType == "json" -> {
                // JSON request body
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                buffer.readUtf8()
            }

            request.body is FormBody -> {

                val formBody = request.body as FormBody

                val paramMap = mutableMapOf<String, Any?>()

                for (i in 0 until formBody.size) {

                    val key = formBody.name(i)
                    val value = formBody.value(i)

                    paramMap[key] = value
                }

                // also include query parameters from URL
                val url = request.url

                for (name in url.queryParameterNames) {
                    paramMap[name] = url.queryParameter(name)
                }

                paramMap.toJsonString()
            }

            request.body is MultipartBody -> {
                val multipart = request.body as MultipartBody
                val paramMap = mutableMapOf<String, Any?>()

                for (part in multipart.parts) {
                    val cd = part.headers?.get("Content-Disposition") ?: continue
                    if (!cd.contains("form-data; name=")) continue

                    val name = cd.substringAfter("name=\"").substringBefore("\"")

                    // skip file/image parts if header contains filename="..."
                    if (cd.contains("filename=\"")) {
                        // skip file/image field (not preserved, no placeholder added)
                        continue
                    }

                    // otherwise read as text part
                    try {
                        val buf = Buffer()
                        part.body.writeTo(buf)
                        val value = buf.readUtf8()
                        if (value.isNotBlank() && name != "eventFile") {
                            paramMap[name] = value
                        }
                    } catch (e: Exception) {
                        // skip field on read failure (prevent interceptor crash)
                        LogUtil.e("read multipart part failed: $name -> ${e.message}")
                    }
                }
                // also merge query parameters from URL
                val url = request.url
                for (name in url.queryParameterNames) {
                    paramMap[name] = url.queryParameter(name)
                }
                (paramMap as Map<*, *>).toJsonString()
            }

            else -> {
                ""
            }
        }
//        LogUtil.e("SIGNN:$bodyJson")
        // generate signature
        val (sign, timestamp) = generateSign(bodyJson)

        // add to request headers
        val newRequest = request.newBuilder()
            .addHeader("sign", sign)
            .addHeader("timestamp", timestamp)
            .build()

        return chain.proceed(newRequest)
    }

    /** Generate sign */
    private fun generateSign(bodyJson: String): Pair<String, String> {
        val timestamp = System.currentTimeMillis().toString()
        val finalJson =
            if (bodyJson.isBlank() || bodyJson == "{}") ApiRequest().toJsonString() else bodyJson
        val sortedJson = sortJson(JSONObject(finalJson)).toJsonString()
//        LogUtil.e("sortedJson:$sortedJson")

        val raw = (APPCODE.toMd5() + "*|*" +
                st + "*|*" +
                sortedJson + "*|*" +
                timestamp).replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
//        LogUtil.e("raw:$raw")
        return Pair(raw.toMd5(), timestamp)
    }

    /** Deep-sort JSONObject into a SortedMap */
    private fun sortJson(json: JSONObject): SortedMap<String, Any?> {
        val sortedMap = TreeMap<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val newValue = when (val value = json.get(key)) {
                is JSONObject -> sortJson(value)
                is JSONArray -> {
                    val list = mutableListOf<Any?>()
                    for (i in 0 until value.length()) {
                        val item = value.get(i)
                        list.add(if (item is JSONObject) sortJson(item) else item)
                    }
                    list
                }

                else -> value
            }
            sortedMap[key] = newValue
        }
        return sortedMap
    }
}
