package com.vaycore.finance.data.network

import com.vaycore.finance.data.APPCODE
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.st
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.util.toMd5
import org.json.JSONArray
import org.json.JSONObject
import java.util.SortedMap
import java.util.TreeMap

/** Generates request-signing headers from the protocol payload. */
class SignatureGenerator(
    private val timestampProvider: () -> String = { System.currentTimeMillis().toString() },
    private val sessionSecretProvider: () -> String = { st }
) {

    fun generate(payload: String): SignatureHeaders {
        val timestamp = timestampProvider()
        val payloadToSign = if (payload.isBlank() || payload == "{}") {
            ApiRequest().toJsonString()
        } else {
            payload
        }
        val sortedPayload = sortJson(JSONObject(payloadToSign)).toJsonString()
        val rawSignature = (
            APPCODE.toMd5() + "*|*" +
                sessionSecretProvider() + "*|*" +
                sortedPayload + "*|*" +
                timestamp
            ).replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")

        return SignatureHeaders(sign = rawSignature.toMd5(), timestamp = timestamp)
    }

    /** Deep-sorts JSON objects while preserving array order. */
    private fun sortJson(json: JSONObject): SortedMap<String, Any?> {
        val sortedMap = TreeMap<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            sortedMap[key] = when (val value = json.get(key)) {
                is JSONObject -> sortJson(value)
                is JSONArray -> (0 until value.length()).map { index ->
                    value.get(index).let { item ->
                        if (item is JSONObject) sortJson(item) else item
                    }
                }
                else -> value
            }
        }
        return sortedMap
    }
}

data class SignatureHeaders(
    val sign: String,
    val timestamp: String
)
