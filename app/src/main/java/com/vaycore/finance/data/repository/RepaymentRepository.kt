package com.vaycore.finance.data.repository

import android.net.Uri
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.bean.RepaymentActionResponse
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.util.generateRequestBody
import com.vaycore.finance.util.uriToPart

class RepaymentRepository(
    private val api: Api,
) {

    suspend fun fetchRepayBankList(): List<BankAccountResponse>? {
        return api.fetchRepayBankList().dataOrThrow()
    }

    suspend fun fetchRepayCardList(): MutableList<BankAccountResponse>? {
        return api.fetchRepayCardList(ApiRequest()).dataOrThrow()
    }

    suspend fun uploadRepaymentVoucher(
        imageCert: Uri,
        orderId: String,
        repayInfoId: String?,
        repayType: String?,
    ): Any? {
        val formMedia = HashMap<String, String>()
        formMedia["mobileType"] = "2"
        formMedia["appCode"] = APPCODE
        formMedia["version"] = BuildConfig.VERSION_NAME
        formMedia["orderId"] = orderId
        if (repayInfoId != null) {
            formMedia["repayInfoId"] = repayInfoId
        }
        if (repayType != null) {
            formMedia["repayType"] = repayType
        }
        return api.repayment(
            imageCert.uriToPart("imagCert"),
            formMedia.generateRequestBody()
        ).dataOrThrow()
    }

    suspend fun fetchBatchRepaymentOrders(): List<ProductBean>? {
        return api.togetherRepaymentList(ApiRequest()).dataOrThrow()
    }

    suspend fun submitBatchRepayment(orderList: List<String>): RepaymentActionResponse? {
        return api.togetherRepayment(ApiRequest(orderNoList = orderList)).dataOrThrow()
    }
}
