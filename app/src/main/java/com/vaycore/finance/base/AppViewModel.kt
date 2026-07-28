package com.vaycore.finance.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaycore.finance.app.App
import com.vaycore.finance.data.ACT_UserAppUserDevice
import com.vaycore.finance.data.ACT_UserAppUserDeviceHasDevice
import com.vaycore.finance.data.bean.ApiResponse
import com.vaycore.finance.data.bean.Event
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.data.isPostDeviceInfo
import com.vaycore.finance.data.st
import com.vaycore.finance.app.AppConfigRepository
import com.vaycore.finance.util.deivce.RiskSnapshotCollector
import com.vaycore.finance.util.PermissionCoordinator
import com.vaycore.finance.util.PermissionScenario
import com.vaycore.finance.util.toJsonString
import kotlinx.coroutines.launch

class AppViewModel(
    private val appConfigRepository: AppConfigRepository = AppConfigRepository(api),
) : BaseViewModel() {
    val errorResponse = MutableLiveData<Event<ApiResponse<*>?>>()
    val isShowLoading = MutableLiveData<Boolean>()

    val secretResult = MutableLiveData<String?>()
    fun getAppSecret() {
        createNetworkRequest { appConfigRepository.fetchAppSecret() }.onSuccess {
            if (!it?.verifySignSecret.isNullOrBlank()) {
                st = it.verifySignSecret
            }
            secretResult.value = it?.verifySignSecret
        }.execute()
    }

    fun hasDeviceInfo(pageString: String, action: (Boolean) -> Unit) {
        if (!isLogin) return
        createNetworkRequest { appConfigRepository.hasUploadedDevice() }.onSuccess {
            submitTrackingEvent(
                TrackBean(
                    p = pageString,
                    act = ACT_UserAppUserDeviceHasDevice,
                    result = it.toJsonString()
                )
            )
            isPostDeviceInfo = it == true
            action.invoke(it == true)
        }.onFailed {
            submitTrackingEvent(
                TrackBean(
                    p = pageString,
                    act = ACT_UserAppUserDeviceHasDevice,
                    result = it.toJsonString()
                )
            )
            isPostDeviceInfo = false
            action(false)
            false
        }
    }

    private var postingDevice: Boolean = false
    fun postRiskInfo(
        pageString: String,
        action: (Boolean) -> Unit
    ) {
        if (!isLogin || !PermissionCoordinator.hasAll(
                App.appContext,
                PermissionScenario.DEVICE_RISK,
            ) || postingDevice
        ) return
        postingDevice = true
        if (isPostDeviceInfo) {
            action(true)
            postingDevice = false
            return
        }
        viewModelScope.launch {
            val riskJson = RiskSnapshotCollector.collect()
            createNetworkRequest {
                appConfigRepository.uploadRiskInfo(riskJson)
            }.showLoading().onSuccess {
                isPostDeviceInfo = true
                submitTrackingEvent(
                    TrackBean(
                        p = pageString,
                        act = ACT_UserAppUserDevice,
                        result = it.toJsonString()
                    )
                )
                action(true)
                postingDevice = false
            }.onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = pageString,
                        act = ACT_UserAppUserDevice,
                        result = it.toJsonString()
                    )
                )
                action(false)
                postingDevice = false
                false
            }
        }

    }
}
