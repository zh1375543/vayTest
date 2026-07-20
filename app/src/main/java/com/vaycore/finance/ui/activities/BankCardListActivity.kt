package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.BankCardListAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityBankCardListBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.viewmodels.WalletViewModel
import com.vaycore.finance.util.WALLET_INFO_PAGE
import com.vaycore.finance.util.start
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.util.viewBinding

class BankCardListActivity :
    BaseActivity<ActivityBankCardListBinding>() {

    override val binding by viewBinding(ActivityBankCardListBinding::inflate)
    private val vm by viewModels<WalletViewModel>()

    private val bankAdapter by lazy {
        BankCardListAdapter().apply {
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
        trackEvent(WALLET_INFO_PAGE)
        rvAccounts.adapter = bankAdapter
        addLayout.singleClick {
            start<BankCardAddActivity>()
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
        accountListResult.observe(this@BankCardListActivity) {
            binding.apply {
                bankAdapter.submitItems(it)
                if (bankAdapter.items.isEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_banklist_null, R.string.empty_bankcard)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
