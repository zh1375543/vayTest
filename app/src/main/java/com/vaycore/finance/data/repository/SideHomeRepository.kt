package com.vaycore.finance.data.repository

import android.net.Uri
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.CreatePlanRequest
import com.vaycore.finance.data.local.sideBean.PlanHomeResponse
import com.vaycore.finance.data.local.sideBean.UploadPlanImageResponse
import com.vaycore.finance.data.network.SidePageApi
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.uriToPart

class SideHomeRepository(
    private val api: SidePageApi,
) {

    suspend fun getPlanHomeData(): PlanHomeResponse? {
        return api.getPlanHomeData(ApiRequest()).dataOrThrow()
    }

    suspend fun addPlan(createPlanRequest: CreatePlanRequest): PlanHomeResponse? {
        return api.addPlan(createPlanRequest).dataOrThrow()
    }

    suspend fun uploadPlanImage(imageUri: Uri): UploadPlanImageResponse? {
        val formMedia = hashMapOf(
            "mobileType" to "2",
            "appCode" to APPCODE,
            "version" to BuildConfig.VERSION_NAME,
            "businessType" to PLAN_IMAGE_TYPE,
        )
        return api.uploadPlanImage(
            imageUri.uriToPart("file"),
            formMedia.generateRequestBody()
        ).dataOrThrow()
    }

    private companion object {
        const val PLAN_IMAGE_TYPE = "planIcon"
    }
}
