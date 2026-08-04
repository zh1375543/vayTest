package com.vaycore.finance.identity.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.ACT_next
import com.vaycore.finance.data.ACT_uploadBack
import com.vaycore.finance.data.ACT_uploadFace
import com.vaycore.finance.data.ACT_uploadFront
import com.vaycore.finance.data.PageInfoKyc
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.identity.data.IdentityVerificationRepository
import com.vaycore.finance.model.identity.KycDocumentResponse
import com.vaycore.finance.model.identity.KycRuleConfigResponse
import com.vaycore.finance.model.identity.LivenessWebSessionResponse
import com.vaycore.finance.model.ui.UiImageSource
import com.vaycore.finance.util.toJsonString
import java.io.File

class KycUploadViewModel(
    private val verificationRepository: IdentityVerificationRepository =
        IdentityVerificationRepository(api),
) : BaseViewModel() {

    val kycResult = MutableLiveData<KycDocumentResponse?>()
    val frontImageSource = MutableLiveData<UiImageSource?>()
    val backImageSource = MutableLiveData<UiImageSource?>()
    val selfImageSource = MutableLiveData<UiImageSource?>()
    val frontUploadSuccess = MutableLiveData(false)
    val backUploadSuccess = MutableLiveData(false)
    val selfUploadSuccess = MutableLiveData(false)

    fun getKycInfo(errorAction: () -> Unit) {
        createNetworkRequest { verificationRepository.fetchKycDocument() }
            .onSuccess {
                kycResult.value = it
                frontImageSource.value = it?.frontImageUrl.toRemoteImageSource()
                backImageSource.value = it?.backImageUrl.toRemoteImageSource()
                selfImageSource.value = it?.liveImageUrl.toRemoteImageSource()
            }
            .onFailed {
                errorAction()
                false
            }
    }

    val h5Live = MutableLiveData<LivenessWebSessionResponse>()
    fun fetchH5Live(error: () -> Unit) {
        createNetworkRequest { verificationRepository.createLivenessWebSession() }
            .showLoading()
            .onSuccess { h5Live.value = it }
            .onFailed {
                error()
                false
            }
    }

    val h5Result = MutableLiveData<String?>()
    fun getH5LiveResult() {
        createNetworkRequest { verificationRepository.fetchLivenessResult(h5Live.value?.bizNo) }
            .showLoading()
            .onSuccess {
                h5Result.value = it?.faceUrl
                selfImageSource.value = it?.faceUrl.toRemoteImageSource()
                selfUploadSuccess.value = !it?.faceUrl.isNullOrBlank()
            }
            .execute()
    }

    val submitFrontResult = MutableLiveData<Uri>()
    fun submitKycFront(frontUri: Uri, cardType: String) {
        createNetworkRequest {
            verificationRepository.uploadKycImage(frontUri, "IDCARD_CARD_FRONT", cardType)
        }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFront,
                        result = it.toJsonString()
                    )
                )
                submitFrontResult.value = frontUri
                frontImageSource.value = UiImageSource.LocalUri(frontUri)
                frontUploadSuccess.value = true
            }
            .onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFront,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }

    val submitBackResult = MutableLiveData<Uri>()
    fun submitKycBack(backUri: Uri, cardType: String) {
        createNetworkRequest {
            verificationRepository.uploadKycImage(backUri, "IDCARD_CARD_BACK", cardType)
        }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadBack,
                        result = it.toJsonString()
                    )
                )
                submitBackResult.value = backUri
                backImageSource.value = UiImageSource.LocalUri(backUri)
                backUploadSuccess.value = true
            }
            .onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadBack,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }

    val configResult = MutableLiveData<KycRuleConfigResponse?>()
    fun getKycConfig() {
        createNetworkRequest { verificationRepository.fetchKycConfig() }
            .onSuccess { configResult.value = it }
            .execute()
    }

    val compareResult = MutableLiveData<Any?>()
    fun compareFace() {
        createNetworkRequest { verificationRepository.compareFace() }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(TrackBean(p = PageInfoKyc, act = ACT_next, result = it.toJsonString()))
                compareResult.value = it
            }
            .onFailed {
                submitTrackingEvent(TrackBean(p = PageInfoKyc, act = ACT_next, result = it.toJsonString()))
                false
            }
    }

    val submitSelfResult = MutableLiveData<Uri?>()
    fun submitKycSelf(uri: Uri, liveFile: File?) {
        createNetworkRequest { verificationRepository.uploadLiveness(uri, liveFile) }
            .showLoading()
            .onSuccess {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFace,
                        result = it.toJsonString()
                    )
                )
                submitSelfResult.value = uri
                selfImageSource.value = UiImageSource.LocalUri(uri)
                selfUploadSuccess.value = true
            }
            .onFailed {
                submitTrackingEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFace,
                        result = it.toJsonString()
                    )
                )
                false
            }
    }

    private fun String?.toRemoteImageSource(): UiImageSource? =
        takeUnless(String?::isNullOrBlank)?.let(UiImageSource::RemoteUrl)
}
