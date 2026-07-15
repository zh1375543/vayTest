package com.vaycore.finance.data.repository

import com.vaycore.finance.data.local.bean.AppSecretResponse
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.network.Api
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AppConfigRepository(
    private val api: Api,
) {

    suspend fun fetchAppSecret(): AppSecretResponse? {
        return api.fetchSecret().dataOrThrow()
    }

    suspend fun hasUploadedDevice(): Boolean? {
        return api.hasUserDevice(ApiRequest()).dataOrThrow()
    }

    suspend fun uploadRiskInfo(riskJson: String): Any? {
        // The exact media type is part of the signed request body.
        val body = riskJson.toRequestBody(
            "application/json; charset=utf-8".toMediaTypeOrNull()
        )
        return api.saveUserDevice(body).dataOrThrow()
    }
}
