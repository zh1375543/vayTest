package com.vaycore.finance.sidepage

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.model.side.CancelPlanRequest
import com.vaycore.finance.model.side.CreatePlanRequest
import com.vaycore.finance.data.bean.Event
import com.vaycore.finance.model.side.PlanCalendarResponse
import com.vaycore.finance.model.side.PlanDetailResponse
import com.vaycore.finance.model.side.PlanHomeResponse
import com.vaycore.finance.model.side.PlanListResponse
import com.vaycore.finance.model.side.RecordListResponse
import com.vaycore.finance.model.side.SavePlanRequest
import com.vaycore.finance.model.side.SavePlanResponse
import com.vaycore.finance.model.side.SavingsReportResponse
import com.vaycore.finance.model.side.UpdatePlanRequest
import com.vaycore.finance.model.side.UploadPlanImageResponse
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
    private val repository: SideHomeRepository = SideHomeRepository(sidePageApi),
) : BaseViewModel() {

    val planHomeResult = MutableLiveData<PlanHomeResponse?>()
    val savingsReportResult = MutableLiveData<SavingsReportResponse?>()
    val planListResult = MutableLiveData<PlanListPage>()
    val planDetailResult = MutableLiveData<PlanDetailResponse?>()
    val planCalendarResult = MutableLiveData<PlanCalendarResponse?>()
    val recordListResult = MutableLiveData<RecordListResponse?>()
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
    val recordListFailed = MutableLiveData<Event<Unit>>()

    private var planHomeJob: Job? = null
    private var savingsReportJob: Job? = null
    private var planListJob: Job? = null
    private var planDetailJob: Job? = null
    private var planCalendarJob: Job? = null
    private var recordListJob: Job? = null
    private var addPlanJob: Job? = null
    private var updatePlanJob: Job? = null
    private var cancelPlanJob: Job? = null
    private var savePlanJob: Job? = null
    private var withdrawPlanJob: Job? = null

    fun getPlanHomeData() {
        planHomeJob?.cancel()
        planHomeJob = createNetworkRequest {
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
        if (!isLogin) return
        savingsReportJob?.cancel()
        savingsReportJob = createNetworkRequest {
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
        planListJob = createNetworkRequest {
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
        planDetailJob = createNetworkRequest {
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
        planCalendarJob = createNetworkRequest {
            repository.getPlanCalendar(year, month)
        }.onSuccess {
            planCalendarResult.value = it
        }.onFailed {
            planCalendarFailed.value = Event(Unit)
            false
        }
    }

    fun getRecordList(planId: Int, startTime: String, endTime: String) {
        recordListJob?.cancel()
        recordListJob = createNetworkRequest {
            repository.getRecordList(planId, startTime, endTime)
        }.onSuccess {
            recordListResult.value = it
        }.onFailed {
            recordListFailed.value = Event(Unit)
            false
        }
    }

    fun uploadPlanImage(imageUri: Uri) {
        createNetworkRequest {
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

        addPlanJob = createNetworkRequest {
            repository.addPlan(request)
        }.showLoading().onSuccess {
            addPlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }

    fun updatePlan(request: UpdatePlanRequest) {
        if (updatePlanJob?.isActive == true) return

        updatePlanJob = createNetworkRequest {
            repository.updatePlan(request)
        }.showLoading().onSuccess {
            updatePlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }

    fun cancelPlan(request: CancelPlanRequest) {
        if (cancelPlanJob?.isActive == true) return

        cancelPlanJob = createNetworkRequest {
            repository.cancelPlan(request)
        }.showLoading().onSuccess {
            cancelPlanResult.value = Event(Unit)
        }.onFailed {
            false
        }
    }

    fun savePlan(request: SavePlanRequest) {
        if (savePlanJob?.isActive == true) return

        savePlanJob = createNetworkRequest {
            repository.savePlan(request)
        }.showLoading().onSuccess { result ->
            result?.let { savePlanResult.value = Event(it) }
        }.onFailed {
            false
        }
    }

    fun withdrawPlan(request: SavePlanRequest) {
        if (withdrawPlanJob?.isActive == true) return

        withdrawPlanJob = createNetworkRequest {
            repository.withdrawPlan(request)
        }.showLoading().onSuccess { result ->
            result?.let { withdrawPlanResult.value = Event(it) }
        }.onFailed {
            false
        }
    }
}
