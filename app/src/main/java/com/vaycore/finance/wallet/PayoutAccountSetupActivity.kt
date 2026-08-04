package com.vaycore.finance.wallet

import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.model.wallet.BankChannelResponse
import com.vaycore.finance.model.wallet.WalletResponse
import com.vaycore.finance.databinding.ActivityPayoutAccountSetupBinding
import com.vaycore.finance.ui.extension.observeKeyboardVisibility
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.identity.viewmodel.PersonalInfoViewModel
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.viewBinding

class PayoutAccountSetupActivity : BaseActivity<ActivityPayoutAccountSetupBinding>() {

    private enum class WithdrawMethod {
        BANK,
        WALLET,
    }

    override val binding by viewBinding(ActivityPayoutAccountSetupBinding::inflate)

    private val vm by viewModels<WalletViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()

    private var selectedWithdrawMethod: WithdrawMethod? = null
    private var bankBean: BankChannelResponse? = null
    private var walletBean: WalletResponse? = null
    private var shouldShowWalletPicker = false

    override fun initView() {
        setupWithdrawMethodSelection()
        setupAccountValidation()
        setupPageActions()
    }

    private fun setupWithdrawMethodSelection() = with(binding.withdrawAccountForm) {
        clearWithdrawMethodSelection()

        methodSelectionView.singleClick {
            showWithdrawMethodDialog(
                walletAction = { selectDefaultWallet() },
                bankAction = { vm.getPayChannelList() },
            )
        }
        bankView.setOnClick { vm.getPayChannelList() }
        walletProviderView.setOnClick {
            shouldShowWalletPicker = true
            vm.getWalletList()
        }
    }

    private fun setupAccountValidation() = with(binding.withdrawAccountForm) {
        bankAccountView.getEditText().doAfterTextChanged {
            bankAccountView.hideError()
            if (it.toString() == confirmBankView.getText()) confirmBankView.hideError()
        }
        confirmBankView.getEditText().doAfterTextChanged {
            if (it.toString() == bankAccountView.getText()) {
                bankAccountView.hideError()
                confirmBankView.hideError()
            }
        }
        walletAccountView.getEditText().doAfterTextChanged {
            walletAccountView.hideError()
            if (it.toString() == confirmWalletAccountView.getText()) {
                confirmWalletAccountView.hideError()
            }
        }
        confirmWalletAccountView.getEditText().doAfterTextChanged {
            if (it.toString() == walletAccountView.getText()) {
                walletAccountView.hideError()
                confirmWalletAccountView.hideError()
            }
        }
    }

    private fun setupPageActions() = with(binding) {
        window.decorView.observeKeyboardVisibility { isShow, _ ->
            if (isShow) {
                tvTips.isVisible = false
            } else {
                tvTips.postDelayed({ tvTips.isVisible = true }, 200)
            }
        }

        tvNext.resetScale()
        tvNext.singleClick {
            when (selectedWithdrawMethod) {
                WithdrawMethod.BANK -> submitBankAccount()
                WithdrawMethod.WALLET -> submitWalletAccount()
                null -> {
                    withdrawAccountForm.tvWithdrawMethodError.isVisible = true
                }
            }
        }

        personalVm.getPersonalInfo {}
    }

    private fun submitBankAccount() {
        with(binding.withdrawAccountForm) {
            if (bankView.getText().isBlank()) {
                bankView.showError()
                return
            }
            if (holderView.getText().isBlank()) {
                holderView.showError()
                return
            }
            if (bankAccountView.getText().isBlank()) {
                bankAccountView.showError()
                return
            }
            if (confirmBankView.getText() != bankAccountView.getText()) {
                confirmBankView.showError()
                return
            }
            vm.addCard(
                bankId = bankBean?.id?.toString(),
                accountUser = holderView.getText(),
                bankNo = bankAccountView.getText(),
            )
        }
    }

    private fun submitWalletAccount() {
        with(binding.withdrawAccountForm) {
            if (walletProviderView.getText().isBlank()) {
                walletProviderView.showError()
                return
            }
            if (walletAccountView.getText().isBlank()) {
                walletAccountView.showError()
                return
            }
            if (confirmWalletAccountView.getText() != walletAccountView.getText()) {
                confirmWalletAccountView.showError()
                return
            }
            vm.addCard(
                bankId = null,
                accountUser = "",
                bankNo = "",
                payWay = "WALLET",
                walletId = walletBean?.id,
                accountCode = walletAccountView.getText().trim(),
            )
        }
    }

    private fun selectWithdrawMethod(method: WithdrawMethod) = with(binding.withdrawAccountForm) {
        selectedWithdrawMethod = method
        tvWithdrawMethodError.isVisible = false
        val iconRes = if (method == WithdrawMethod.BANK) {
            R.mipmap.ic_bank_select_bg
        } else {
            R.mipmap.ic_wallet_select_bg
        }
        val iconSize = resources.getDimensionPixelSize(R.dimen.dp_36)
        val icon = AppCompatResources.getDrawable(this@PayoutAccountSetupActivity, iconRes)?.apply {
            setBounds(0, 0, iconSize, iconSize)
        }
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@PayoutAccountSetupActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(icon, null, arrow, null)
        methodSelectionView.text = getString(
            if (method == WithdrawMethod.BANK) R.string.bank else R.string.e_wallet,
        )
        bankFieldsLayout.isVisible = method == WithdrawMethod.BANK
        walletFieldsLayout.isVisible = method == WithdrawMethod.WALLET
    }

    private fun clearWithdrawMethodSelection() = with(binding.withdrawAccountForm) {
        selectedWithdrawMethod = null
        shouldShowWalletPicker = false
        walletBean = null
        tvWithdrawMethodError.isVisible = false
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@PayoutAccountSetupActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(null, null, arrow, null)
        methodSelectionView.text = getString(R.string.please_select)
        bankFieldsLayout.isVisible = false
        walletFieldsLayout.isVisible = false
    }

    private fun selectDefaultWallet() = with(binding.withdrawAccountForm) {
        shouldShowWalletPicker = false
        walletBean = null
        selectWithdrawMethod(WithdrawMethod.WALLET)
        walletProviderView.setText(getString(R.string.gcash))
        walletProviderView.hideError()
        vm.getWalletList()
    }

    private fun applyWalletSelection(wallet: WalletResponse) = with(binding.withdrawAccountForm) {
        selectWithdrawMethod(WithdrawMethod.WALLET)
        walletProviderView.setText(wallet.walletName)
        walletProviderView.hideError()
        walletBean = wallet
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        payChannelList.observe(this@PayoutAccountSetupActivity) {
            chooseBankDialog(it ?: emptyList()) { bean ->
                selectWithdrawMethod(WithdrawMethod.BANK)
                binding.withdrawAccountForm.bankView.setText(bean.bankName)
                binding.withdrawAccountForm.bankView.hideError()
                bankBean = bean
            }
        }
        walletList.observe(this@PayoutAccountSetupActivity) {
            val walletItems = it ?: emptyList()
            if (shouldShowWalletPicker) {
                shouldShowWalletPicker = false
                chooseWalletDialog(walletItems) { wallet ->
                    applyWalletSelection(wallet)
                }
            } else if (selectedWithdrawMethod == WithdrawMethod.WALLET) {
                walletBean = walletItems.firstOrNull {
                    it.walletName.equals(getString(R.string.gcash), ignoreCase = true)
                }
            }
        }
        addResult.observe(this@PayoutAccountSetupActivity) {
            getString(R.string.toast_add_account_receivable).showToastMessage()
            finish()
        }
        personalVm.personalResult.observe(this@PayoutAccountSetupActivity) {
            binding.withdrawAccountForm.holderView.setText(it?.firstName)
        }
    }
}
