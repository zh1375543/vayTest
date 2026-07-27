package com.vaycore.finance.identity.data

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.model.identity.UserAuthStatusResponse

class AuthStatusRepository(
    private val api: Api,
) {

    suspend fun loadUserAuthStatus(): UserAuthStatusResponse? {
        return api.fetchUserAuth(ApiRequest()).dataOrThrow()
    }

    suspend fun loadAuthConfigList(): List<String> {
        return api.fetchAuthentication()
            .dataOrThrow()
            ?.authConfig
            ?.split(",")
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }
}
