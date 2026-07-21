package com.vaycore.finance.data.network

import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.ApiResponse
import com.vaycore.finance.data.local.bean.CreatePlanRequest
import com.vaycore.finance.data.local.sideBean.PlanHomeResponse
import com.vaycore.finance.data.local.sideBean.UploadPlanImageResponse
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

    @Multipart
    @POST("api/user/attachment/upload")
    suspend fun uploadPlanImage(
        @Part file: MultipartBody.Part,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<UploadPlanImageResponse?>
}
