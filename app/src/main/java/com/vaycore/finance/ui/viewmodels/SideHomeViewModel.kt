package com.vaycore.finance.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.sideBean.CancelPlanRequest
import com.vaycore.finance.data.local.sideBean.CreatePlanRequest
import com.vaycore.finance.data.local.bean.Event
import com.vaycore.finance.data.local.sideBean.PlanCalendarResponse
import com.vaycore.finance.data.local.sideBean.PlanDetailResponse
import com.vaycore.finance.data.local.sideBean.PlanHomeResponse
import com.vaycore.finance.data.local.sideBean.PlanListResponse
import com.vaycore.finance.data.local.sideBean.SavePlanRequest
import com.vaycore.finance.data.local.sideBean.SavePlanResponse
import com.vaycore.finance.data.local.sideBean.SavingsReportResponse
import com.vaycore.finance.data.local.sideBean.UpdatePlanRequest
import com.vaycore.finance.data.local.sideBean.UploadPlanImageResponse
import com.vaycore.finance.data.repository.SideHomeRepository
import kotlinx.coroutines.Job

sealed interface PlanImageUploadState {
    data class Success(val result: UploadPlanImageResponse) : PlanImageUploadState
    data class Failed(val message: String?) : PlanImageUploadState
}

data class PlanListPage(
    val status: Int?,
    val pageNum: Int,
    val response: PlanListResponse?,
)

class SideHomeViewModel(
    private val repository: SideHomeRepository = SideHomeRepository(BaseViewModel.sidePageApi),
) : BaseViewModel() {

    val planHomeResult = MutableLiveData<PlanHomeResponse?>()
    val savingsReportResult = MutableLiveData<SavingsReportResponse?>()
    val planListResult = MutableLiveData<PlanListPage>()
    val planDetailResult = MutableLiveData<PlanDetailResponse?>()
    val planCalendarResult = MutableLiveData<PlanCalendarResponse?>()
    val planImageUploadState = MutableLiveData<PlanImageUploadState>()
    val addPlanResult = MutableLiveData<Event<Unit>>()
    val updatePlanResult = MutableLiveData<Event<Unit>>()
    val cancelPlanResult = MutableLiveData<Event<Unit>>()
    val savePlanResult = MutableLiveData<Event<SavePlanResponse>>()
    val withdrawPlanResult = MutableLiveData<Event<SavePlanResponse>>()
    val requestCompleted = MutableLiveData<Unit>()
    val planHomeFailed = MutableLiveData<Event<Unit>>()
    val savingsReportFailed = MutableLiveData<Event<Unit>>()
    val planListFailed = MutableLiveData<Event<PlanListPage>>()
    val planDetailFailed = MutableLiveData<Event<Unit>>()
    val planCalendarFailed = MutableLiveData<Event<Unit>>()

    private var planHomeJob: Job? = null
    private var savingsReportJob: Job? = null
    private var planListJob: Job? = null
    private var planDetailJob: Job? = null
    private var planCalendarJob: Job? = null
    private var addPlanJob: Job? = null
    private var updatePlanJob: Job? = null
    private var cancelPlanJob: Job? = null
    private var savePlanJob: Job? = null
    private var withdrawPlanJob: Job? = null

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

    fun saveReport() {
        savingsReportJob?.cancel()
        savingsReportJob = launchData {
            repository.saveReport()
        }.onSuccess {
            savingsReportResult.value = it
        }.onFailed {
            savingsReportFailed.value = Event(Unit)
            false
        }
    }

    fun getPlanList(status: Int?, pageNum: Int, pageSize: Int) {
        planListJob?.cancel()
        planListJob = launchData {
            repository.getPlanList(status, pageNum, pageSize)
        }.onSuccess { result ->
            planListResult.value = PlanListPage(status, pageNum, result)
        }.onFailed {
            planListFailed.value = Event(PlanListPage(status, pageNum, null))
            false
        }
    }

    fun getPlanDetail(planId: Int) {
        planDetailJob?.cancel()
        planDetailJob = launchData {
            repository.getPlanDetail(planId)
        }.onSuccess {
            planDetailResult.value = it
        }.onFailed {
            planDetailFailed.value = Event(Unit)
            false
        }
    }

    fun getPlanCalendar(year: Int, month: Int) {
        planCalendarJob?.cancel()
        planCalendarJob = launchData {
            repository.getPlanCalendar(year, month)
        }.onSuccess {
            planCalendarResult.value = it
        }.onFailed {
            planCalendarFailed.value = Event(Unit)
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

    fun updatePlan(request: UpdatePlanRequest) {
        if (updatePlanJob?.isActive == true) return

        updatePlanJob = launchData {
            repository.updatePlan(request)
        }.showLoading().onSuccess {
            updatePlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }

    fun cancelPlan(request: CancelPlanRequest) {
        if (cancelPlanJob?.isActive == true) return

        cancelPlanJob = launchData {
            repository.cancelPlan(request)
        }.showLoading().onSuccess {
            cancelPlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }

    fun savePlan(request: SavePlanRequest) {
        if (savePlanJob?.isActive == true) return

        savePlanJob = launchData {
            repository.savePlan(request)
        }.showLoading().onSuccess { result ->
            result?.let { savePlanResult.value = Event(it) }
        }.onFailed {
            false
        }
    }

    fun withdrawPlan(request: SavePlanRequest) {
        if (withdrawPlanJob?.isActive == true) return

        withdrawPlanJob = launchData {
            repository.withdrawPlan(request)
        }.showLoading().onSuccess { result ->
            result?.let { withdrawPlanResult.value = Event(it) }
        }.onFailed {
            false
        }
    }
}
