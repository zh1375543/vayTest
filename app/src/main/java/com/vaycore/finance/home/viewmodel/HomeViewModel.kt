package com.vaycore.finance.home.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_common
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.home.data.DefaultHomeRepository
import com.vaycore.finance.home.data.HomeRepository
import com.vaycore.finance.model.home.CampaignBannerResponse
import com.vaycore.finance.model.home.GuestHomeResponse
import com.vaycore.finance.util.toJsonString

/** Owns data requests and transient screen state that are exclusive to HomeFragment. */
class HomeViewModel(
    private val homeRepository: HomeRepository = DefaultHomeRepository(api),
) : BaseViewModel() {

    val guestResult = MutableLiveData<GuestHomeResponse?>()
    val loadFailedResult = MutableLiveData<Unit>()
    val bannerResult = MutableLiveData<List<CampaignBannerResponse>>()

    fun getUnAuthData(showLoading: Boolean = false) {
        createNetworkRequest {
            homeRepository.loadGuestHome()
        }.showLoading(showLoading).onSuccess {
            guestResult.value = it
            submitTrackingEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString(),
                ),
            )
        }.onFailed {
            loadFailedResult.value = Unit
            submitTrackingEvent(
                TrackBean(
                    p = PageHome,
                    act = ACT_common,
                    result = it.toJsonString(),
                ),
            )
            true
        }
    }

    fun getBannerList() {
        createNetworkRequest {
            homeRepository.loadBanners()
        }.onSuccess {
            bannerResult.value = it ?: emptyList()
        }.onFailed { true }
    }
}
