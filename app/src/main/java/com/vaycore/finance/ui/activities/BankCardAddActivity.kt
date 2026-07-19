package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.databinding.ActivityBankCardAddBinding
import com.vaycore.finance.ui.chooseBankDialog
import com.vaycore.finance.ui.chooseWalletDialog
import com.vaycore.finance.ui.extension.observeKeyboardVisibility
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showWithdrawMethodDialog
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.viewBinding

class BankCardAddActivity : BaseActivity<ActivityBankCardAddBinding>() {

    private enum class WithdrawMethod {
        BANK,
        WALLET,
    }

    override val binding by viewBinding(ActivityBankCardAddBinding::inflate)

    private val vm by viewModels<WalletViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()

    private var selectedWithdrawMethod: WithdrawMethod? = null
    private var bankBean: BankChannelResponse? = null
    private var walletBean: WalletResponse? = null

    override fun initView() = with(binding) {
        with(withdrawAccountForm) {
        clearWithdrawMethodSelection()

        methodSelectionView.singleClick {
            showWithdrawMethodDialog(
                walletAction = { vm.getWalletList() },
                bankAction = { vm.getPayChannelList() },
            )
        }
        bankView.setOnClick { vm.getPayChannelList() }
        walletProviderView.setOnClick { vm.getWalletList() }

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
                    tvWithdrawMethodError.isVisible = true
                    methodSelectionView.performClick()
                }
            }
        }

            personalVm.getPersonalInfo {}
        }
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
        val icon = AppCompatResources.getDrawable(this@BankCardAddActivity, iconRes)?.apply {
            setBounds(0, 0, iconSize, iconSize)
        }
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@BankCardAddActivity, R.mipmap.mine_right)?.apply {
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
        tvWithdrawMethodError.isVisible = false
        val arrowSize = resources.getDimensionPixelSize(R.dimen.dp_24)
        val arrow = AppCompatResources.getDrawable(this@BankCardAddActivity, R.mipmap.mine_right)?.apply {
            setBounds(0, 0, arrowSize, arrowSize)
        }
        methodSelectionView.setCompoundDrawablesRelative(null, null, arrow, null)
        methodSelectionView.text = getString(R.string.please_select)
        bankFieldsLayout.isVisible = false
        walletFieldsLayout.isVisible = false
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        payChannelList.observe(this@BankCardAddActivity) {
            chooseBankDialog(it ?: emptyList()) { bean ->
                selectWithdrawMethod(WithdrawMethod.BANK)
                binding.withdrawAccountForm.bankView.setText(bean.bankName)
                binding.withdrawAccountForm.bankView.hideError()
                bankBean = bean
            }
        }
        walletList.observe(this@BankCardAddActivity) {
            chooseWalletDialog(it ?: emptyList()) { wallet ->
                selectWithdrawMethod(WithdrawMethod.WALLET)
                binding.withdrawAccountForm.walletProviderView.setText(wallet.walletName)
                binding.withdrawAccountForm.walletProviderView.hideError()
                walletBean = wallet
            }
        }
        addResult.observe(this@BankCardAddActivity) {
            getString(R.string.toast_add_account_receivable).showToastMessage()
            finish()
        }
        personalVm.personalResult.observe(this@BankCardAddActivity) {
            binding.withdrawAccountForm.holderView.setText(it?.firstName)
        }
    }
}
