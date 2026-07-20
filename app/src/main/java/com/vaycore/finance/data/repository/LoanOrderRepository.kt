package com.vaycore.finance.data.repository

import com.vaycore.finance.data.local.bean.LoanOrderDetailResponse
import com.vaycore.finance.data.local.bean.OrderBean
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.local.bean.RepaymentActionResponse
import com.vaycore.finance.data.network.Api

class LoanOrderRepository(
    private val api: Api,
) {

    suspend fun fetchOrderList(): List<OrderBean> {
        return api.fetchOrderList().dataOrThrow() ?: emptyList()
    }

    suspend fun fetchOrderDetail(orderId: Long?): LoanOrderDetailResponse? {
        return api.fetchOrderDetail(ApiRequest(orderId = orderId)).dataOrThrow()
    }

    suspend fun fetchRepaymentBorrowButtonState(): String? {
        return api.showRepaymentBorrow().dataOrThrow()?.reloanButtonSign
    }

    suspend fun installmentRepay(
        orderNo: String?,
        planNumberList: List<Int?>?,
    ): RepaymentActionResponse? {
        return api.installmentRepay(
            ApiRequest(
                orderNo = orderNo,
                planNumList = planNumberList,
            )
        ).dataOrThrow()
    }

    suspend fun repayAndBorrow(orderId: Long?, applyAgainSign: Int?): Any? {
        return api.repayAndBorrow(
            ApiRequest(orderId = orderId, applyAgainSign = applyAgainSign)
        ).dataOrThrow()
    }

    suspend fun cancelApply(orderId: Long?): Any? {
        return api.cancelApply(ApiRequest(orderId = orderId)).dataOrThrow()
    }
}
