package com.vaycore.finance.home.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_common
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.home.data.DefaultHomeRepository
import com.vaycore.finance.home.data.HomeRepository
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.util.toJsonString

/** Loads guest configuration reused by login and customer-support entry points. */
class GuestDashboardViewModel(
    private val homeRepository: HomeRepository = DefaultHomeRepository(api),
) : BaseViewModel() {

    val result = MutableLiveData<GuestHomeResponse?>()
    val loadFailedResult = MutableLiveData<Unit>()

    fun getUnAuthData(showLoading: Boolean = false) {
        launchData {
            homeRepository.loadGuestHome()
        }.showLoading(showLoading).onSuccess {
            result.value = it
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString(),
                ),
            )
        }.onFailed {
            loadFailedResult.value = Unit
            recordEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString(),
                ),
            )
            true
        }
    }
}
