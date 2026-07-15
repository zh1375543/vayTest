package com.vaycore.finance.data.repository

import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.network.Api

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
