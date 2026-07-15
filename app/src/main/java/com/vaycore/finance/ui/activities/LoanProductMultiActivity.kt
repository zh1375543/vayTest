package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import com.vaycore.finance.ui.adapters.ComboAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityLoanProductMultiBinding
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.ui.viewmodels.LoanApplyViewModel
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.util.LoanEventUtil
import com.vaycore.finance.util.formatAmount
import com.vaycore.finance.util.maskSensitive
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.toJsonString
import com.vaycore.finance.ui.chooseAccountsDialog
import com.vaycore.finance.ui.showLoanAgreementDialog
import com.vaycore.finance.util.deviceRiskPermissions
import com.vaycore.finance.util.viewBinding

class LoanProductMultiActivity :
    BaseActivity<ActivityLoanProductMultiBinding>() {

    override val binding by viewBinding(ActivityLoanProductMultiBinding::inflate)
    private val togetherAdapter by lazy {
        ComboAdapter()
    }

    private val vm by viewModels<LoanApplyViewModel>()
    private val accountVm by viewModels<WalletViewModel>()

    private var cardInfo: BankAccountResponse? = null

    private val loanEvent by lazy { LoanEventUtil.instance }

    override fun initView() = with(binding) {
        titleBar.setNavigationAction { onBackPressed() }
        rvProduct.adapter = togetherAdapter
        tvChange.singleClick {
            loanEvent.logClickChooseWallet()
            accountVm.getBankcardList { }
        }
        tvApply.singleClick {
            loanEvent.logClickApplyLoan()
//                LogUtil.e("productInstallmentMap:" + togetherAdapter.productInstallmentMap.toJsonString())
//                LogUtil.e("termMap:" + togetherAdapter.termMap.toJsonString())
            requestRuntimePermissions(deviceRiskPermissions) {
                showLoanAgreementDialog(true) {
                    loanEvent.logClickSubmitLoan()
                    LoanApplyResultActivity.launch(
                        this@LoanProductMultiActivity,
                        ArrayList(togetherAdapter.items),
                        null,
                        cardInfo?.id,
                        null,
                        null,
                        if (togetherAdapter.productInstallmentMap.isEmpty()) null else togetherAdapter.productInstallmentMap.toJsonString(),
                        if (togetherAdapter.termMap.isEmpty()) null else togetherAdapter.termMap.toJsonString()
                    )
                    finish()
                }
            }
        }

        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getTogetherLoan {
                loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loanEvent.initBaseServerTime(System.currentTimeMillis())
        loanEvent.logViewEnterLoan()
        binding.loadingLayout.showLoading()
        vm.getTogetherLoan {
            binding.loadingLayout.showError()
        }
    }


    override fun initObserve() = with(vm) {
        super.initObserve()
        togetherInfo.observe(this@LoanProductMultiActivity) {
            it?.let { loan ->
                binding.loadingLayout.showContent()
                if (cardInfo == null) {
                    cardInfo = BankAccountResponse(id = loan.bankInfoId, bankNo = loan.bankNo)
                    binding.tvCard.text = cardInfo?.bankNo.maskSensitive()
                }
                togetherAdapter.submitItemsWithState(loan.showProducts?.onEach { it1 ->
                    it1.canApply = true
                    it1.isTogether = true
                })
//                lifecycleScope.launch(Dispatchers.IO) {
//                    loan.showProducts.toJsonString().writeJsonToCacheFile()
//                }
                binding.tvNum.text = togetherAdapter.items.size.toString()
                binding.tvAmount.text =
                    loan.canApplyAmount.formatAmount(loan.currencySymbol)
            }
        }
        accountVm.bankCardListResult.observe(this@LoanProductMultiActivity) {
            it?.let {
                chooseAccountsDialog(cardInfo?.bankNo, it, false) { card ->
                    cardInfo = card
                    binding.tvCard.text = card.bankNo.maskSensitive()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        loanEvent.logViewQuitLoan()
        loanEvent.writeLog2File()
    }
}
