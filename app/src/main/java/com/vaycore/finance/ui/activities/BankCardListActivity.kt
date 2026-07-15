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
                when (view.id) {
                    R.id.tvDefault -> showConfirmDialog(
                        getString(R.string.set_default_title),
                        getString(R.string.set_default_desc),
                        getString(R.string.closed),
                        getString(R.string.sure),
                        okAction = {
                            vm.setDefaultCard(items[position].id.toString()) {
                                items.forEach { item ->
                                    item.isDefault = 0
                                }
                                items[position].isDefault = 1
                                notifyItemRangeChanged(0, itemCount, 0)
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
                                items[position].id.toString()
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
            vm.getBankcardList {
                binding.loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getBankcardList {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        bankCardListResult.observe(this@BankCardListActivity) {
            binding.apply {
                bankAdapter.submitItems(it)
                if (bankAdapter.items.isEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_accounts_null, R.string.empty_bankcard)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
