package com.vaycore.finance.wallet

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.model.wallet.BankChannelResponse
import com.vaycore.finance.model.wallet.WalletResponse

class WalletRepository(
    private val api: Api,
) {

    suspend fun fetchPayChannelList(): List<BankChannelResponse>? {
        return api.fetchPayChannel(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchWalletList(): List<WalletResponse>? {
        return api.getWalletList(ApiRequest()).dataOrThrow()
    }

    suspend fun fetchMyWalletList(): List<WalletResponse>? {
        return api.getMyWalletList(ApiRequest()).dataOrThrow()
    }

    suspend fun addCard(
        bankId: String?,
        accountUser: String,
        bankNo: String,
        payWay: String = "CARD",
        walletId: Int? = null,
        accountCode: String? = null,
    ): Any? {
        return api.addCard(
            ApiRequest(
                bankId = bankId,
                accountUser = accountUser,
                bankNo = bankNo,
                payWay = payWay,
                walletId = walletId,
                accountCode = accountCode,
            )
        ).dataOrThrow()
    }

    suspend fun fetchBankcardList(): List<BankAccountResponse>? {
        return api.fetchBankcardList(ApiRequest()).dataOrThrow()
    }

    suspend fun unbindCard(bankInfoId: String, payWay: String = "CARD"): Any? {
        return api.unbindCard(
            ApiRequest(
                bankInfoId = bankInfoId,
                payWay = payWay,
            )
        ).dataOrThrow()
    }

    suspend fun setDefaultCard(bankInfoId: String): Any? {
        return api.fetchCardDefault(ApiRequest(bankInfoId = bankInfoId)).dataOrThrow()
    }

    suspend fun setDefaultWallet(id: Int?): Any? {
        return api.setDefaultWallet(ApiRequest(id = id)).dataOrThrow()
    }
}