package com.vaycore.finance.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.EnterTime
import com.vaycore.finance.data.bean.SurveyBean
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.bean.TrackParamBean
import com.vaycore.finance.data.pCount
import com.vaycore.finance.data.network.*
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.util.DataNetworkRequest
import com.vaycore.finance.util.toJsonString
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Base ViewModel class */
abstract class BaseViewModel : ViewModel() {

    /** Launch a network request returning unwrapped business data */
    fun <T> createNetworkRequest(block: suspend () -> T?): DataNetworkRequest<T> {
        return DataNetworkRequest(viewModelScope, block)
    }

    /** Submit a single analytics event */
    fun submitTrackingEvent(log: TrackBean) {
        submitTrackingEvents(listOf(log))
    }

    /** Submit analytics events after enriching each one with navigation context */
    fun submitTrackingEvents(logs: List<TrackBean>) {
        if (logs.isEmpty()) return

        logs.forEach { log ->
            pCount++
            log.apply {
                pp = "$EnterTime;$pCount"
                prevAct = lastTrackBean?.act
                prevP = lastTrackBean?.p
                lastP = lastPageTrackBean?.p
                lastAct = lastPageTrackBean?.act
            }

            lastTrackBean = log
            log.takeIf { it.p != null && it.p != lastPageTrackBean?.p }?.let {
                lastPageTrackBean = it
            }
        }

        createNetworkRequest {
            trackApi.submitTrack(TrackParamBean(logs.map { SurveyBean(it.toJsonString()) })).dataOrThrow()
        }.onSuccess { }.onFailed { true }
    }

    companion object {
        private var lastTrackBean: TrackBean? = null
        private var lastPageTrackBean: TrackBean? = null

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(NetworkInterceptor())
                .addInterceptor(LogInterceptor())
                .addInterceptor(ParamsInterceptor())
                .addInterceptor(HeaderInterceptor())
                .addInterceptor(SignInterceptor())
                .addInterceptor(TrackInterceptor())
                .connectTimeout(30L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .writeTimeout(60L, TimeUnit.SECONDS)
                .build()
        }

        val api: Api by lazy { createService(Api::class.java, BuildConfig.HTTP_HOST) }

        val sidePageApi: SidePageApi by lazy {
            createService(SidePageApi::class.java, BuildConfig.HTTP_HOST)
        }

        val trackApi: ApiTrack by lazy { createService(ApiTrack::class.java, BuildConfig.TRACK_HOST) }

        private fun <T> createService(
            serviceClass: Class<T>,
            baseUrl: String
        ): T {
            return Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .build()
                .create(serviceClass)
        }
    }
}
