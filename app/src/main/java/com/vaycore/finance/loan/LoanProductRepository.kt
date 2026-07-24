package com.vaycore.finance.loan

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow

class LoanProductRepository(
    private val api: Api,
) {

    suspend fun fetchProductDetail(
        productId: String?,
        amount: String?,
    ): ProductBean? {
        return api.fetchProductDetail(
            ApiRequest(
                productId = productId,
                amount = amount,
            )
        ).dataOrThrow()
    }
}
