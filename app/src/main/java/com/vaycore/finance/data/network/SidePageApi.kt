package com.vaycore.finance.data.network

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.bean.ApiResponse
import com.vaycore.finance.model.side.CancelPlanRequest
import com.vaycore.finance.model.side.CreatePlanRequest
import com.vaycore.finance.model.side.PlanCalendarRequest
import com.vaycore.finance.model.side.PlanCalendarResponse
import com.vaycore.finance.model.side.PlanDetailRequest
import com.vaycore.finance.model.side.PlanDetailResponse
import com.vaycore.finance.model.side.PlanHomeResponse
import com.vaycore.finance.model.side.PlanListRequest
import com.vaycore.finance.model.side.PlanListResponse
import com.vaycore.finance.model.side.SavePlanRequest
import com.vaycore.finance.model.side.SavePlanResponse
import com.vaycore.finance.model.side.SavingsReportResponse
import com.vaycore.finance.model.side.UpdatePlanRequest
import com.vaycore.finance.model.side.UploadPlanImageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface SidePageApi {

    @POST("api/user/app/saving/plan/home")
    suspend fun getPlanHomeData(@Body param: ApiRequest): ApiResponse<PlanHomeResponse?>


    @POST("api/user/app/saving/plan/create")
    suspend fun addPlan(@Body param: CreatePlanRequest): ApiResponse<PlanHomeResponse?>

    @POST("api/user/app/saving/plan/cancel")
    suspend fun cancelPlan(@Body param: CancelPlanRequest): ApiResponse<Any?>

    @Multipart
    @POST("api/user/attachment/upload")
    suspend fun uploadPlanImage(
        @Part file: MultipartBody.Part,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<UploadPlanImageResponse?>


    @POST("api/user/app/saving/plan/detail")
    suspend fun getPlanDetail(@Body param: PlanDetailRequest): ApiResponse<PlanDetailResponse?>

    @POST("api/user/app/saving/calendar/query")
    suspend fun  getPlanCalendar(@Body param: PlanCalendarRequest): ApiResponse<PlanCalendarResponse?>

    @POST("api/user/app/saving/plan/save")
    suspend fun  savePlan(@Body param: SavePlanRequest): ApiResponse<SavePlanResponse?>

    @POST("api/user/app/saving/plan/withdraw")
    suspend fun  withdrawPlan(@Body param: SavePlanRequest): ApiResponse<SavePlanResponse?>

    @POST("api/user/app/saving/plan/update")
    suspend fun  updatePlan(@Body param: UpdatePlanRequest): ApiResponse<Any?>

    @POST("api/user/app/saving/report/query")
    suspend fun saveReport(@Body param: ApiRequest): ApiResponse<SavingsReportResponse?>

    @POST("api/user/app/saving/plan/list")
    suspend fun getPlanList(@Body param: PlanListRequest): ApiResponse<PlanListResponse?>

}
