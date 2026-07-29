package com.vaycore.finance.wallet

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.wallet.adapter.PayoutAccountAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityPayoutAccountListBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.util.WALLET_INFO_PAGE
import com.vaycore.finance.util.start
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding

class PayoutAccountListActivity :
    BaseActivity<ActivityPayoutAccountListBinding>() {

    override val binding by viewBinding(ActivityPayoutAccountListBinding::inflate)
    private val vm by viewModels<WalletViewModel>()

    private val bankAdapter by lazy {
        PayoutAccountAdapter().apply {
            setOnChildClickListener { view, _, position ->
                val account = items[position]
                when (view.id) {
                    R.id.tvDefault -> showConfirmDialog(
                        getString(R.string.set_default_title),
                        getString(R.string.set_default_desc),
                        getString(R.string.closed),
                        getString(R.string.sure),
                        okAction = {
                            val updateDefaultState = {
                                items.filter { it.payWay == account.payWay }.forEach {
                                    it.isDefault = 0
                                }
                                account.isDefault = 1
                                notifyItemRangeChanged(0, itemCount, 0)
                            }
                            if (account.payWay == "WALLET") {
                                vm.setDefaultWallet(account.id?.toInt(), updateDefaultState)
                            } else {
                                vm.setDefaultCard(account.id.toString(), updateDefaultState)
                            }
                        },
                        cancelAction = {}
                    )

                    R.id.tvDelete -> showConfirmDialog(
                        getString(R.string.unbind),
                        getString(R.string.unbind_desc),
                        getString(R.string.closed),
                        getString(R.string.sure),
                        okAction = {
                            vm.unBindCard(
                                account.id.toString(),
                                account.payWay ?: "CARD",
                            ) {
                                removeItem(position)
                            }
                        },
                        cancelAction = {}
                    )
                }
            }
        }
    }

    override fun initView() = with(binding) {
        viewModel = vm
        trackEvent(WALLET_INFO_PAGE)
        rvAccounts.adapter = bankAdapter
        addLayout.singleClick {
            start<PayoutAccountSetupActivity>()
        }
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getAccountList {
                binding.loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getAccountList {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        accountListResult.observe(this@PayoutAccountListActivity) {
            binding.apply {
                rvAccounts.isVisible = !it.isNullOrEmpty()
                if (it.isNullOrEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_banklist_null, R.string.empty_bankcard)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
