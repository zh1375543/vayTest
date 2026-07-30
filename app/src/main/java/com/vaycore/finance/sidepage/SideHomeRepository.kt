package com.vaycore.finance.sidepage

import android.net.Uri
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.APPCODE
import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.model.side.CancelPlanRequest
import com.vaycore.finance.model.side.CreatePlanRequest
import com.vaycore.finance.model.side.PlanCalendarRequest
import com.vaycore.finance.model.side.PlanCalendarResponse
import com.vaycore.finance.model.side.PlanDetailRequest
import com.vaycore.finance.model.side.PlanDetailResponse
import com.vaycore.finance.model.side.PlanHomeResponse
import com.vaycore.finance.model.side.PlanListRequest
import com.vaycore.finance.model.side.PlanListResponse
import com.vaycore.finance.model.side.RecordListRequest
import com.vaycore.finance.model.side.RecordListResponse
import com.vaycore.finance.model.side.SavePlanRequest
import com.vaycore.finance.model.side.SavePlanResponse
import com.vaycore.finance.model.side.SavingsReportResponse
import com.vaycore.finance.model.side.UpdatePlanRequest
import com.vaycore.finance.model.side.UploadPlanImageResponse
import com.vaycore.finance.data.network.SidePageApi
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.uriToPart

class SideHomeRepository(
    private val api: SidePageApi,
) {

    suspend fun getPlanHomeData(): PlanHomeResponse? {
        return api.getPlanHomeData(ApiRequest()).dataOrThrow()
    }

    suspend fun saveReport(): SavingsReportResponse? {
        return api.saveReport(ApiRequest()).dataOrThrow()
    }

    suspend fun getPlanList(
        status: Int? = null,
        pageNum: Int = 1,
        pageSize: Int = DEFAULT_PLAN_LIST_PAGE_SIZE,
    ): PlanListResponse? {
        return api.getPlanList(
            PlanListRequest(
                status = status,
                pageNum = pageNum,
                pageSize = pageSize,
            ),
        ).dataOrThrow()
    }

    suspend fun getPlanDetail(planId: Int): PlanDetailResponse? {
        return api.getPlanDetail(PlanDetailRequest(id = planId.toString())).dataOrThrow()
    }

    suspend fun getPlanCalendar(year: Int, month: Int): PlanCalendarResponse? {
        return api.getPlanCalendar(
            PlanCalendarRequest(
                year = year,
                month = month,
            ),
        ).dataOrThrow()
    }

    suspend fun getRecordList(
        planId: Int,
        startTime: String,
        endTime: String,
    ): RecordListResponse? {
        return api.getRecordList(
            RecordListRequest(
                planId = planId,
                startTime = startTime,
                endTime = endTime,
            ),
        ).dataOrThrow()
    }

    suspend fun addPlan(createPlanRequest: CreatePlanRequest): PlanHomeResponse? {
        return api.addPlan(createPlanRequest).dataOrThrow()
    }

    suspend fun updatePlan(request: UpdatePlanRequest): Any? {
        return api.updatePlan(request).dataOrThrow()
    }

    suspend fun cancelPlan(request: CancelPlanRequest): Any? {
        return api.cancelPlan(request).dataOrThrow()
    }

    suspend fun savePlan(request: SavePlanRequest): SavePlanResponse? {
        return api.savePlan(request).dataOrThrow()
    }

    suspend fun withdrawPlan(request: SavePlanRequest): SavePlanResponse? {
        return api.withdrawPlan(request).dataOrThrow()
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
        const val DEFAULT_PLAN_LIST_PAGE_SIZE = 10
    }
}
