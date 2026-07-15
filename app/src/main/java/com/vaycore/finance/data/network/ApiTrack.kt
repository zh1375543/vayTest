package com.vaycore.finance.data.network

import com.vaycore.finance.data.local.bean.ApiResponse
import com.vaycore.finance.data.local.bean.TrackParamBean
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiTrack {

    @POST("track")
    suspend fun submitTrack(@Body paramBean: TrackParamBean): ApiResponse<Any?>
}
