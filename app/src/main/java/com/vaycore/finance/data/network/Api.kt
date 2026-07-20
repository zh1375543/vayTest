package com.vaycore.finance.data.network

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.ApiResponse
import com.vaycore.finance.data.local.bean.AddressRegionResponse
import com.vaycore.finance.data.local.bean.AuthOptionResponse
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.CampaignBannerResponse
import com.vaycore.finance.data.local.bean.WorkContactProfileResponse
import com.vaycore.finance.data.local.bean.GuestHomeResponse
import com.vaycore.finance.data.local.bean.LoanDashboardResponse
import com.vaycore.finance.data.local.bean.KycRuleConfigResponse
import com.vaycore.finance.data.local.bean.LivenessWebSessionResponse
import com.vaycore.finance.data.local.bean.KycDocumentResponse
import com.vaycore.finance.data.local.bean.LoginSessionResponse
import com.vaycore.finance.data.local.bean.MessageListPageResponse
import com.vaycore.finance.data.local.bean.OrderBean
import com.vaycore.finance.data.local.bean.LoanOrderDetailResponse
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.data.local.bean.PersonalProfileResponse
import com.vaycore.finance.data.local.bean.PersonalProfileOptionsResponse
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.AppSecretResponse
import com.vaycore.finance.data.local.bean.RepaymentActionResponse
import com.vaycore.finance.data.local.bean.UserAuthStatusResponse
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.data.local.bean.WorkProfileOptionsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface Api {

    @GET("api/user/app/common/secret")
    suspend fun fetchSecret(): ApiResponse<AppSecretResponse?>

    @POST("api/user/app/login/sms")
    suspend fun sendSMS(@Body param: ApiRequest): ApiResponse<Any?>

    @POST("api/loan/app/common/index")
    suspend fun fetchHomeData(@Body param: ApiRequest = ApiRequest()): ApiResponse<GuestHomeResponse?>

    @POST("api/user/app/login")
    suspend fun login(@Body param: ApiRequest): ApiResponse<LoginSessionResponse?>

    @POST("api/user/app/delete/user")
    suspend fun logout(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/password/set")
    suspend fun fetchPassword(@Body param: ApiRequest): ApiResponse<LoginSessionResponse?>

    @POST("api/user/app/password/update")
    suspend fun updatePassword(@Body param: ApiRequest): ApiResponse<LoginSessionResponse?>

    @POST("api/user/app/userEquipment/save")
    suspend fun postDeviceInfo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/userAuth/detail")
    suspend fun fetchUserAuth(@Body paramBean: ApiRequest = ApiRequest()): ApiResponse<UserAuthStatusResponse?>

    @POST("api/user/app/kyc/info")
    suspend fun fetchKycInfo(@Body paramBean: ApiRequest): ApiResponse<KycDocumentResponse?>

    @POST("api/user/app/userBaseExt/getEnum")
    suspend fun fetchPersonalInfoEnum(@Body paramBean: ApiRequest): ApiResponse<PersonalProfileOptionsResponse?>

    @POST("api/user/app/userBaseExt/info")
    suspend fun fetchPersonalInfo(@Body paramBean: ApiRequest): ApiResponse<PersonalProfileResponse?>

    @POST("api/user/app/address/list")
    suspend fun fetchAddressList(@Body paramBean: ApiRequest): ApiResponse<MutableList<AddressRegionResponse>?>

    @POST("api/user/app/userBaseExt/save/v2")
    suspend fun postPersonalInfo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/userWork/enum")
    suspend fun fetchWorkInfoEnum(@Body paramBean: ApiRequest): ApiResponse<WorkProfileOptionsResponse?>

    @POST("api/user/app/userWork/info")
    suspend fun fetchContactInfo(@Body param: ApiRequest): ApiResponse<WorkContactProfileResponse?>

    @POST("api/user/app/userWork/save")
    suspend fun postContactInfo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/list")
    suspend fun fetchPayChannel(@Body paramBean: ApiRequest): ApiResponse<MutableList<BankChannelResponse>?>

    @POST("api/user/app/wallet/list")
    suspend fun getWalletList(@Body param: ApiRequest): ApiResponse<MutableList<WalletResponse>?>

    @POST("api/user/app/userCashWallet/list/my")
    suspend fun getMyWalletList(@Body param: ApiRequest): ApiResponse<MutableList<WalletResponse>?>

    @POST("api/user/app/userCashWallet/set/default")
    suspend fun setDefaultWallet(@Body param: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/bind")
    suspend fun bindCard(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/myCard")
    suspend fun fetchBankcardList(@Body paramBean: ApiRequest): ApiResponse<MutableList<BankAccountResponse>?>

    @POST("api/user/app/bank/addBank")
    suspend fun addCard(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/unbind")
    suspend fun unbindCard(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/setDefault")
    suspend fun fetchCardDefault(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/loan/app/index/v3")
    suspend fun fetchHomeLoan(@Body paramBean: ApiRequest = ApiRequest()): ApiResponse<LoanDashboardResponse?>

    @Multipart
    @POST("api/loan/app/order/commit/all/with/event")
    suspend fun oneLoanApply(
        @Part files: List<MultipartBody.Part>,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<MutableList<ProductBean>?>

    @Multipart
    @POST("api/loan/app/order/commit/with/event")
    suspend fun loanApply(
        @Part files: List<MultipartBody.Part>,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<ProductBean?>

    @POST("api/loan/app/productInfo/detail")
    suspend fun fetchProductDetail(@Body paramBean: ApiRequest): ApiResponse<ProductBean?>

    @POST("api/loan/app/order/oldList")
    suspend fun fetchOrderList(@Body paramBean: ApiRequest = ApiRequest()): ApiResponse<MutableList<OrderBean>?>

    @POST("api/loan/app/order/detail")
    suspend fun fetchOrderDetail(@Body paramBean: ApiRequest): ApiResponse<LoanOrderDetailResponse?>

    @POST("api/data/app/fcm/sendRecord/list")
    suspend fun fetchMessageList(@Body paramBean: ApiRequest): ApiResponse<MessageListPageResponse?>

    @POST("api/data/app/fcm/sendRecord/update")
    suspend fun updateMessageStatus(@Body param: ApiRequest): ApiResponse<Any?>

    @FormUrlEncoded
    @POST("api/user/app/bank/repay/list")
    suspend fun fetchRepayBankList(
        @Field("appCode") appCode: String = APPCODE,
        @Field("version") version: String = BuildConfig.VERSION_NAME,
        @Field("mobileType") mobileType: String = "2",
    ): ApiResponse<MutableList<BankAccountResponse>?>

    @POST("api/user/app/bank/getBankAndWallet")
    suspend fun fetchRepayCardList(@Body paramBean: ApiRequest): ApiResponse<MutableList<BankAccountResponse>?>

    @Multipart
    @POST("api/finance/app/upload/cert")
    suspend fun repayment(
        @Part files: MultipartBody.Part,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<Any?>

    @GET("api/user/app/application/config/auth/config")
    suspend fun fetchAuthentication(): ApiResponse<AuthOptionResponse?>

    @POST("api/loan/app/index/loan/page")
    suspend fun togetherLoan(@Body param: ApiRequest = ApiRequest()): ApiResponse<LoanDashboardResponse?>

    @POST("api/user/app/userDevice/save")
    suspend fun saveUserDevice(@Body riskRequestBody: RequestBody): ApiResponse<Any?>

    @POST("api/user/app/userDevice/hasDevice")
    suspend fun hasUserDevice(@Body paramBean: ApiRequest): ApiResponse<Boolean?>

    @POST("api/user/app/vietnam/telecom/otp/one")
    suspend fun fetchTeleOtpOne(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/vietnam/telecom/otp/two")
    suspend fun fetchTeleOtpTwo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/vietnam/telecom/otp/three")
    suspend fun submitTeleOtp(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/bank/bind/v2")
    suspend fun submitBankAndContactInfo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @GET("api/user/app/activity/list")
    suspend fun fetchBannerList(): ApiResponse<MutableList<CampaignBannerResponse>?>

    @POST("api/user/app/userBaseExt/save/work/v2")
    suspend fun submitSuppleInfo(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/finance/app/multiple/order/list")
    suspend fun togetherRepaymentList(@Body paramBean: ApiRequest): ApiResponse<MutableList<ProductBean>?>

    @POST("api/finance/app/multiple/order/repay")
    suspend fun togetherRepayment(@Body paramBean: ApiRequest): ApiResponse<RepaymentActionResponse?>

    @GET("api/user/app/common/reloan/button/sign")
    suspend fun showRepaymentBorrow(): ApiResponse<RepaymentActionResponse?>

    @POST("api/finance/app/order/repay/url")
    suspend fun installmentRepay(@Body paramBean: ApiRequest): ApiResponse<RepaymentActionResponse?>

    @POST("api/loan/app/apply/again")
    suspend fun repayAndBorrow(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/loan/app/apply/again/cancel")
    suspend fun cancelApply(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/kyc/config")
    suspend fun fetchKycConfig(@Body paramBean: ApiRequest): ApiResponse<KycRuleConfigResponse?>

    @POST("api/user/app/kyc/face/compare")
    suspend fun faceCompare(@Body paramBean: ApiRequest): ApiResponse<Any?>

    @Multipart
    @POST("api/user/app/kyc/save/v2")
    suspend fun submitKycImage(
        @Part image: MultipartBody.Part,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<Any?>

    @Multipart
    @POST("api/user/app/kyc/liveness/anti/hack")
    suspend fun submitLiveness(
        @Part livenessDataFile: MultipartBody.Part?,
        @Part faceFile: MultipartBody.Part,
        @PartMap multipartBody: Map<String, @JvmSuppressWildcards RequestBody>,
    ): ApiResponse<Any?>

    @POST("api/user/app/score/review")
    suspend fun submitFeedback(@Body feedback: ApiRequest): ApiResponse<Any?>

    @POST("api/user/app/user/liveness/result/get")
    suspend fun getH5Result(@Body p: ApiRequest): ApiResponse<LivenessWebSessionResponse?>

    @POST("api/user/app/user/liveness/h5/get")
    suspend fun fetchH5Live(@Body p: ApiRequest): ApiResponse<LivenessWebSessionResponse?>
}
