package com.vaycore.finance.data.repository

import android.net.Uri
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.KycDocumentResponse
import com.vaycore.finance.data.local.bean.KycRuleConfigResponse
import com.vaycore.finance.data.local.bean.LivenessWebSessionResponse
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.PersonalProfileOptionsResponse
import com.vaycore.finance.data.local.bean.PersonalProfileResponse
import com.vaycore.finance.data.local.bean.SelectionOption
import com.vaycore.finance.data.local.bean.WorkContactProfileResponse
import com.vaycore.finance.data.local.bean.WorkProfileOptionsResponse
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.uriToPart
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

class IdentityVerificationRepository(
    private val api: Api,
) {

    suspend fun fetchKycDocument(): KycDocumentResponse? {
        return api.fetchKycInfo(ApiRequest()).dataOrThrow()
    }

    suspend fun createLivenessWebSession(): LivenessWebSessionResponse? {
        return api.fetchH5Live(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchLivenessResult(bizNo: String?): LivenessWebSessionResponse? {
        return api.getH5Result(ApiRequest(bizNo = bizNo)).dataOrThrow()
    }

    suspend fun uploadKycImage(imageUri: Uri, imageType: String): Any? {
        val formMedia = HashMap<String, String>()
        formMedia["mobileType"] = "2"
        formMedia["appCode"] = APPCODE
        formMedia["version"] = BuildConfig.VERSION_NAME
        formMedia["imgType"] = imageType
        return api.submitKycImage(
            imageUri.uriToPart("image"),
            formMedia.generateRequestBody()
        ).dataOrThrow()
    }

    suspend fun fetchKycConfig(): KycRuleConfigResponse? {
        return api.fetchKycConfig(ApiRequest()).dataOrThrow()
    }

    suspend fun compareFace(): Any? {
        return api.faceCompare(ApiRequest()).dataOrThrow()
    }

    suspend fun uploadLiveness(faceUri: Uri, liveFile: File?): Any? {
        val formMedia = HashMap<String, String>()
        formMedia["mobileType"] = "2"
        formMedia["appCode"] = APPCODE
        formMedia["version"] = BuildConfig.VERSION_NAME
        formMedia["imageId"] = UUID.randomUUID().toString()
        var livePart: MultipartBody.Part? = null
        if (liveFile != null) {
            val requestBody = liveFile.asRequestBody("image/*".toMediaTypeOrNull())
            livePart = MultipartBody.Part.createFormData(
                "livenessDataFile",
                liveFile.name,
                requestBody
            )
        }
        return api.submitLiveness(
            livePart,
            faceUri.uriToPart("faceFile"),
            formMedia.generateRequestBody()
        ).dataOrThrow()
    }

    suspend fun submitPersonalInfo(param: ApiRequest): Any? {
        return api.postPersonalInfo(param).dataOrThrow()
    }

    suspend fun fetchPersonalInfoOptions(): PersonalProfileOptionsResponse? {
        return api.fetchPersonalInfoEnum(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchAddressOptions(parentId: String?): List<SelectionOption> {
        return api.fetchAddressList(ApiRequest(parentId = parentId))
            .dataOrThrow()
            ?.map { SelectionOption(it.name.orEmpty(), id = it.id) }
            ?: emptyList()
    }

    suspend fun fetchWorkInfoOptions(): WorkProfileOptionsResponse? {
        return api.fetchWorkInfoEnum(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchContactInfo(): WorkContactProfileResponse? {
        return api.fetchContactInfo(ApiRequest()).dataOrThrow()
    }

    suspend fun submitBankAndContactInfo(param: ApiRequest): Any? {
        return api.submitBankAndContactInfo(param).dataOrThrow()
    }

    suspend fun requestCarrierOtp(phone: String, company: String): Any? {
        return api.fetchTeleOtpOne(ApiRequest(phone = phone, company = company)).dataOrThrow()
    }

    suspend fun submitCarrierOtp(phone: String, company: String, otp: String): Any? {
        return api.submitTeleOtp(
            ApiRequest(phone = phone, company = company, otp = otp)
        ).dataOrThrow()
    }

    suspend fun submitCarrierOtpAndRequestNext(
        phone: String,
        company: String,
        otp: String,
    ): Any? {
        return api.fetchTeleOtpTwo(
            ApiRequest(phone = phone, company = company, otp = otp)
        ).dataOrThrow()
    }

    suspend fun submitSupplementInfo(param: ApiRequest): Any? {
        return api.submitSuppleInfo(param).dataOrThrow()
    }

    suspend fun fetchPersonalInfo(): PersonalProfileResponse? {
        return api.fetchPersonalInfo(ApiRequest()).dataOrThrow()
    }
}
