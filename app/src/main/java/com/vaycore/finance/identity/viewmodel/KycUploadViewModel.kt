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
import com.vaycore.finance.identity.IdentityVerificationRepository
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
        launchData { verificationRepository.fetchKycDocument() }
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
        launchData { verificationRepository.createLivenessWebSession() }
            .showLoading()
            .onSuccess { h5Live.value = it }
            .onFailed {
                error()
                false
            }
    }

    val h5Result = MutableLiveData<String?>()
    fun getH5LiveResult() {
        launchData { verificationRepository.fetchLivenessResult(h5Live.value?.bizNo) }
            .showLoading()
            .onSuccess {
                h5Result.value = it?.faceUrl
                selfImageSource.value = it?.faceUrl.toRemoteImageSource()
                selfUploadSuccess.value = !it?.faceUrl.isNullOrBlank()
            }
            .execute()
    }

    val submitFrontResult = MutableLiveData<Uri>()
    fun submitKycFront(frontUri: Uri) {
        launchData { verificationRepository.uploadKycImage(frontUri, "IDCARD_CARD_FRONT") }
            .showLoading()
            .onSuccess {
                recordEvent(
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
                recordEvent(
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
    fun submitKycBack(backUri: Uri) {
        launchData { verificationRepository.uploadKycImage(backUri, "IDCARD_CARD_BACK") }
            .showLoading()
            .onSuccess {
                recordEvent(
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
                recordEvent(
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
        launchData { verificationRepository.fetchKycConfig() }
            .onSuccess { configResult.value = it }
            .execute()
    }

    val compareResult = MutableLiveData<Any?>()
    fun compareFace() {
        launchData { verificationRepository.compareFace() }
            .showLoading()
            .onSuccess {
                recordEvent(TrackBean(p = PageInfoKyc, act = ACT_next, result = it.toJsonString()))
                compareResult.value = it
            }
            .onFailed {
                recordEvent(TrackBean(p = PageInfoKyc, act = ACT_next, result = it.toJsonString()))
                false
            }
    }

    val submitSelfResult = MutableLiveData<Uri?>()
    fun submitKycSelf(uri: Uri, liveFile: File?) {
        launchData { verificationRepository.uploadLiveness(uri, liveFile) }
            .showLoading()
            .onSuccess {
                recordEvent(
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
                recordEvent(
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