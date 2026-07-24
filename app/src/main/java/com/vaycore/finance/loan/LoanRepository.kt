package com.vaycore.finance.loan

import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import okhttp3.MultipartBody
import okhttp3.RequestBody

class LoanRepository(
    private val api: Api,
) {

    suspend fun submitTogetherLoan(
        files: List<MultipartBody.Part>,
        multipartBody: Map<String, RequestBody>,
    ): List<ProductBean>? {
        return api.oneLoanApply(files, multipartBody).dataOrThrow()
    }

    suspend fun submitLoan(
        files: List<MultipartBody.Part>,
        multipartBody: Map<String, RequestBody>,
    ): ProductBean? {
        return api.loanApply(files, multipartBody).dataOrThrow()
    }

    suspend fun fetchTogetherLoan(): LoanDashboardResponse? {
        return api.togetherLoan().dataOrThrow()
    }
}
