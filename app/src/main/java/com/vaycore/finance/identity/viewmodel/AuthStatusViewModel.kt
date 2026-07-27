package com.vaycore.finance.identity.viewmodel

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.isLogin
import com.vaycore.finance.identity.data.AuthStatusRepository
import com.vaycore.finance.model.identity.UserAuthStatusResponse

/** Provides authentication progress and the server-configured authentication steps. */
class AuthStatusViewModel(
    private val authStatusRepository: AuthStatusRepository = AuthStatusRepository(api),
) : BaseViewModel() {

    val userAuthStatusResult = MutableLiveData<UserAuthStatusResponse?>()
    val loadFailedResult = MutableLiveData<Unit>()

    fun getUserAuthStatus(errorAction: () -> Unit = {}) {
        createNetworkRequest {
            authStatusRepository.loadUserAuthStatus()
        }.onSuccess {
            userAuthStatusResult.value = it
        }.onFailed {
            loadFailedResult.value = Unit
            errorAction()
            false
        }
    }

    fun fetchAuthConfigList(action: (List<String>) -> Unit) {
        if (!isLogin) return
        createNetworkRequest {
            authStatusRepository.loadAuthConfigList()
        }.onSuccess { list ->
            action(list.orEmpty())
        }.execute()
    }
}
