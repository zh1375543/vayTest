package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.data.repository.WalletRepository

class WalletViewModel(
    private val walletRepository: WalletRepository = WalletRepository(BaseViewModel.api),
) : BaseViewModel() {

    var payChannelList = MutableLiveData<List<BankChannelResponse>?>()
    fun getPayChannelList() {
        if (payChannelList.value != null) {
            payChannelList.value = payChannelList.value
            return
        }
        launchData {
            walletRepository.fetchPayChannelList()
        }.showLoading().onSuccess {
            payChannelList.value = it
        }.execute()
    }

    val walletList = MutableLiveData<List<WalletResponse>?>()
    fun getWalletList() {
        if (walletList.value != null) {
            walletList.value = walletList.value
            return
        }
        launchData {
            walletRepository.fetchWalletList()
        }.showLoading().onSuccess {
            walletList.value = it
        }.execute()
    }

    val addResult = MutableLiveData<Any?>()
    fun addCard(
        bankId: String?,
        accountUser: String,
        bankNo: String,
        payWay: String = "CARD",
        walletId: Int? = null,
        accountCode: String? = null,
    ) {
        launchData {
            walletRepository.addCard(
                bankId = bankId,
                accountUser = accountUser,
                bankNo = bankNo,
                payWay = payWay,
                walletId = walletId,
                accountCode = accountCode,
            )
        }.showLoading().onSuccess {
            addResult.value = it
        }.execute()
    }

    val bankCardListResult = MutableLiveData<List<BankAccountResponse>>()
    fun getBankcardList(errorAction: () -> Unit) {
        launchData {
            walletRepository.fetchBankcardList()
        }.onSuccess {
            bankCardListResult.value = it
        }.onFailed {
            errorAction()
            true
        }
    }

    val accountListResult = MutableLiveData<List<BankAccountResponse>>()
    fun getAccountList(errorAction: () -> Unit) {
        launchData {
            walletRepository.fetchBankcardList()
        }.onSuccess { cards ->
            loadWalletAccounts(cards.orEmpty(), errorAction)
        }.onFailed {
            errorAction()
            true
        }
    }

    private fun loadWalletAccounts(
        cards: List<BankAccountResponse>,
        errorAction: () -> Unit,
    ) {
        launchData {
            walletRepository.fetchMyWalletList()
        }.onSuccess { wallets ->
            val bankAccounts = cards.map { card ->
                card.copy(payWay = "CARD")
            }
            val walletAccounts = wallets.orEmpty().map { wallet ->
                wallet.toBankAccountResponse()
            }
            accountListResult.value = bankAccounts + walletAccounts
        }.onFailed {
            errorAction()
            true
        }
    }

    val loanAccountList = MutableLiveData<List<BankAccountResponse>>()
    fun getLoanAccountList(errorAction: () -> Unit) {
        launchData {
            walletRepository.fetchMyWalletList()
        }.onSuccess { wallets ->
            loadLoanBankAccounts(wallets.orEmpty(), errorAction)
        }.onFailed {
            loadLoanBankAccounts(emptyList(), errorAction)
            true
        }
    }

    private fun loadLoanBankAccounts(
        wallets: List<WalletResponse>,
        errorAction: () -> Unit,
    ) {
        launchData {
            walletRepository.fetchBankcardList()
        }.onSuccess { cards ->
            val walletAccounts = wallets.map { wallet -> wallet.toBankAccountResponse() }
            val bankAccounts = cards.orEmpty().map { card ->
                card.copy(payWay = "CARD")
            }
            loanAccountList.value = walletAccounts + bankAccounts
        }.onFailed {
            errorAction()
            true
        }
    }

    fun unBindCard(id: String, payWay: String = "CARD", action: () -> Unit) {
        launchData {
            walletRepository.unbindCard(id, payWay)
        }.showLoading().onSuccess {
            action.invoke()
        }.execute()
    }

    fun setDefaultCard(id: String, action: () -> Unit) {
        launchData {
            walletRepository.setDefaultCard(id)
        }.showLoading().onSuccess {
            action.invoke()
        }.execute()
    }

    fun setDefaultWallet(id: Int?, action: () -> Unit) {
        launchData {
            walletRepository.setDefaultWallet(id)
        }.showLoading().onSuccess {
            action.invoke()
        }.execute()
    }

    private fun WalletResponse.toBankAccountResponse() = BankAccountResponse(
        id = id.toLong(),
        bankNo = accountCode,
        bankName = walletName.orEmpty(),
        isDefault = defaultSign ?: 0,
        payWay = "WALLET",
    )
}
