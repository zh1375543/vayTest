package com.vaycore.finance.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hjq.permissions.XXPermissions
import com.vaycore.finance.App
import com.vaycore.finance.data.ACT_UserAppUserDevice
import com.vaycore.finance.data.ACT_UserAppUserDeviceHasDevice
import com.vaycore.finance.data.local.bean.ApiResponse
import com.vaycore.finance.data.local.bean.Event
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.data.local.isPostDeviceInfo
import com.vaycore.finance.data.local.st
import com.vaycore.finance.data.repository.AppConfigRepository
import com.vaycore.finance.util.runtime.DeviceCollectHelper
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.toJsonString
import kotlinx.coroutines.launch

class AppViewModel(
    private val appConfigRepository: AppConfigRepository = AppConfigRepository(BaseViewModel.api),
) : BaseViewModel() {
    val errorResponse = MutableLiveData<Event<ApiResponse<*>?>>()
    val isShowLoading = MutableLiveData<Boolean>()

    val secretResult = MutableLiveData<String?>()
    fun getAppSecret() {
        launchData { appConfigRepository.fetchAppSecret() }.onSuccess {
            if (!it?.verifySignSecret.isNullOrBlank()) {
                st = it.verifySignSecret
            }
            secretResult.value = it?.verifySignSecret
        }.execute()
    }

    fun hasDeviceInfo(pageString: String, action: (Boolean) -> Unit) {
        if (!isLogin) return
        launchData { appConfigRepository.hasUploadedDevice() }.onSuccess {
            recordEvent(
                TrackBean(
                    p = pageString,
                    act = ACT_UserAppUserDeviceHasDevice,
                    result = it.toJsonString()
                )
            )
            isPostDeviceInfo = it == true
            action.invoke(it == true)
        }.onFailed {
            recordEvent(
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
        if (!isLogin || !XXPermissions.isGrantedPermissions(
                App.appContext,
                deviceRiskPermissions
            )
            || postingDevice
        ) return
        postingDevice = true
        if (isPostDeviceInfo) {
            action(true)
            postingDevice = false
            return
        }
        viewModelScope.launch {
            val riskJson = DeviceCollectHelper.getInstance().getRiskBy2Json()
            launchData {
                appConfigRepository.uploadRiskInfo(riskJson)
            }.showLoading().onSuccess {
                isPostDeviceInfo = true
                recordEvent(
                    TrackBean(
                        p = pageString,
                        act = ACT_UserAppUserDevice,
                        result = it.toJsonString()
                    )
                )
                action(true)
                postingDevice = false
            }.onFailed {
                recordEvent(
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
