package com.vaycore.finance.util

import com.google.gson.JsonSyntaxException
import com.vaycore.finance.App
import com.vaycore.finance.data.local.bean.ApiResponse
import com.vaycore.finance.data.local.bean.Event
import com.vaycore.finance.data.repository.ApiResponseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException

class DataNetworkRequest<T>(
    private val viewModelScope: CoroutineScope,
    private val block: suspend () -> T?,
) {
    private var showLoading: Boolean = false
    private var onSuccess: (suspend (T?) -> Unit)? = null
    private var onFailed: (suspend (ApiResponse<*>) -> Boolean) = { false }

    fun showLoading(show: Boolean = true): DataNetworkRequest<T> {
        this.showLoading = show
        return this
    }

    fun onSuccess(block: suspend (T?) -> Unit): DataNetworkRequest<T> {
        this.onSuccess = block
        return this
    }

    fun onFailed(block: suspend (ApiResponse<*>) -> Boolean): Job {
        this.onFailed = block
        return execute()
    }

    fun execute(): Job = viewModelScope.launch {
        if (showLoading) App.appViewModel.isShowLoading.postValue(true)
        try {
            val response = withContext(Dispatchers.IO) { block() }
            onSuccess?.invoke(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleException(e)
        } finally {
            if (showLoading) App.appViewModel.isShowLoading.postValue(false)
        }
    }

    private suspend fun handleException(e: Exception) {
        if (e is ApiResponseException) {
            processError(e.response)
            return
        }
        if (e is JsonSyntaxException && e.message?.contains("End of input") == true) {
            LogUtil.e("Network Empty Body: ignored")
            return
        }
        LogUtil.e("Network Exception: ${e.message}")
        processError(parseException(e))
    }

    private suspend fun processError(errorBean: ApiResponse<*>) {
        errorBean.disabledToast = onFailed(errorBean)
        App.appViewModel.errorResponse.postValue(Event(errorBean))
    }

    private fun parseException(e: Exception): ApiResponse<*> {
        if (e !is HttpException) {
            return ApiResponse<Any?>(code = -1, message = e.message, disabledToast = true)
        }

        val errorBody = e.response()?.errorBody()?.string()
        if (errorBody.isNullOrBlank()) {
            return ApiResponse<Any?>(code = e.code(), message = e.message(), disabledToast = true)
        }

        return try {
            gson.fromJson(errorBody, ApiResponse::class.java)
                ?: ApiResponse(code = e.code(), message = e.message(), disabledToast = true)
        } catch (_: JsonSyntaxException) {
            LogUtil.e("Parse Error Body Failed: $errorBody")
            ApiResponse<Any?>(code = e.code(), message = e.message(), disabledToast = true)
        }
    }
}
