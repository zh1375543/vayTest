package com.vaycore.finance.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.CreatePlanRequest
import com.vaycore.finance.data.local.bean.Event
import com.vaycore.finance.data.local.sideBean.PlanHomeResponse
import com.vaycore.finance.data.local.sideBean.UploadPlanImageResponse
import com.vaycore.finance.data.repository.SideHomeRepository
import kotlinx.coroutines.Job

sealed interface PlanImageUploadState {
    data class Success(val result: UploadPlanImageResponse) : PlanImageUploadState
    data class Failed(val message: String?) : PlanImageUploadState
}

class SideHomeViewModel(
    private val repository: SideHomeRepository = SideHomeRepository(BaseViewModel.sidePageApi),
) : BaseViewModel() {

    val planHomeResult = MutableLiveData<PlanHomeResponse?>()
    val planImageUploadState = MutableLiveData<PlanImageUploadState>()
    val addPlanResult = MutableLiveData<Event<Unit>>()
    val requestCompleted = MutableLiveData<Unit>()
    val planHomeFailed = MutableLiveData<Event<Unit>>()

    private var planHomeJob: Job? = null
    private var addPlanJob: Job? = null

    fun getPlanHomeData() {
        planHomeJob?.cancel()
        planHomeJob = launchData {
            repository.getPlanHomeData()
        }.onSuccess {
            planHomeResult.value = it
            requestCompleted.value = Unit
        }.onFailed {
            requestCompleted.value = Unit
            planHomeFailed.value = Event(Unit)
            false
        }
    }

    fun uploadPlanImage(imageUri: Uri) {
        launchData {
            repository.uploadPlanImage(imageUri)
        }.showLoading().onSuccess { result ->
            if (result != null && result.imageUrl.isNotBlank()) {
                planImageUploadState.value = PlanImageUploadState.Success(result)
            } else {
                planImageUploadState.value = PlanImageUploadState.Failed(null)
            }
        }.onFailed { response ->
            planImageUploadState.value = PlanImageUploadState.Failed(response.message)
            true
        }
    }

    fun addPlan(request: CreatePlanRequest) {
        if (addPlanJob?.isActive == true) return

        addPlanJob = launchData {
            repository.addPlan(request)
        }.showLoading().onSuccess {
            addPlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }
}
