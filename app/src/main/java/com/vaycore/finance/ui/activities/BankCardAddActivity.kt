package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.databinding.ActivityBankCardAddBinding
import com.vaycore.finance.ui.extension.observeKeyboardVisibility
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.chooseBankDialog
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.ui.viewmodels.PersonalInfoViewModel
import com.vaycore.finance.util.viewBinding

class BankCardAddActivity :
    BaseActivity<ActivityBankCardAddBinding>() {

    override val binding by viewBinding(ActivityBankCardAddBinding::inflate)

    private val vm by viewModels<WalletViewModel>()
    private val personalVm by viewModels<PersonalInfoViewModel>()

    override fun initView() = with(binding) {
        bankView.setOnClick {
            vm.getPayChannelList()
        }
        bankAccountView.getEditText().doAfterTextChanged {
            bankAccountView.hideError()
            if (it.toString() == confirmBankView.getText()) {
                confirmBankView.hideError()
            }
        }
        confirmBankView.getEditText().doAfterTextChanged {
            if (it.toString() == bankAccountView.getText()) {
                bankAccountView.hideError()
                confirmBankView.hideError()
            }
        }
        window.decorView.observeKeyboardVisibility { isShow, _ ->
            if (isShow) {
                tvTips.isVisible = false
            } else {
                tvTips.postDelayed({
                    tvTips.isVisible = true
                }, 200)
            }
        }
        tvNext.resetScale()
        tvNext.singleClick {
            if (bankView.getText().isBlank()) {
                bankView.showError()
                return@singleClick
            }
            if (holderView.getText().isBlank()) {
                holderView.showError()
                return@singleClick
            }
            if (bankAccountView.getText().isBlank()) {
                bankAccountView.showError()
                return@singleClick
            }
            if (confirmBankView.getText() != bankAccountView.getText()) {
                confirmBankView.showError()
                return@singleClick
            }
            vm.addCard(
                bankId = bankBean?.id.toString(),
                accountUser = holderView.getText(),
                bankNo = bankAccountView.getText(),
            )
        }
        personalVm.getPersonalInfo {}
    }

    private var bankBean: BankChannelResponse? = null
    override fun initObserve() =with(vm){
        super.initObserve()
        payChannelList.observe(this@BankCardAddActivity) {
            chooseBankDialog(
                it ?: arrayListOf()
            ) { bean ->
                binding.bankView.setText(bean.bankName)
                binding.bankView.hideError()
                bankBean = bean
            }
        }
        addResult.observe(this@BankCardAddActivity) {
            getString(R.string.toast_add_account_receivable).showToastMessage()
            finish()
        }
        personalVm.personalResult.observe(this@BankCardAddActivity) {
            binding.holderView.setText(it?.firstName)
        }
    }
}
