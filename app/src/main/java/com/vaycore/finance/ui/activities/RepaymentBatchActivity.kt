package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.BatchRepaymentAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.RepaymentBatchActivityBinding
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.formatAmount
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.viewmodels.RepayViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding
import java.math.BigDecimal

class RepaymentBatchActivity :
    BaseActivity<RepaymentBatchActivityBinding>() {

    override val binding by viewBinding(RepaymentBatchActivityBinding::inflate)
    private val vm by viewModels<RepayViewModel>()

    private val orderAdapter by lazy {
        BatchRepaymentAdapter().apply {
            setOnItemClickListener { item, position ->
                item.isCheck = !item.isCheck
                notifyItemRangeChanged(position, 1, 0)
                val selectList = items.filter { it1 -> it1.isCheck }
                binding.tvNum.text = selectList.size.toString()
                binding.tvAmount.text = selectList.fold(
                    BigDecimal.ZERO
                ) { acc, order -> acc + (order.actualRepayAmount ?: BigDecimal.ZERO) }
                    .formatAmountWithPrefix(item.currencySymbol)
            }
        }
    }

    override fun initView() = with(binding) {
        rvOrder.adapter = orderAdapter
        tvApply.singleClick {
            if (orderAdapter.items.none { it1 -> it1.isCheck }) {
                getString(R.string.toast_empty_choose_repayment).showToastMessage()
                return@singleClick
            }
            vm.togetherRepayment(
                orderAdapter.items.map { it.orderNo ?: "" }
            )
        }
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getOrderList {
                loadingLayout.showError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getOrderList {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        orderListResult.observe(this@RepaymentBatchActivity) {
            binding.apply {
                loadingLayout.showContent()
                orderAdapter.submitItems(it?.onEach { it1 ->
                    it1.isCheck = true
//                        filter { it1 ->
//                            it1.orderStatus == ORDER_STATUS_PAYMENT_PENDING
//                                    || it1.orderStatus == ORDER_STATUS_IN_RENEWAL
//                                    || it1.orderStatus == ORDER_STATUS_IN_RENEWAL_PROCESS
//                        }
                })
                if (it.isNullOrEmpty()) {
                    tvNum.text = "0"
                    tvAmount.text = "0"
                } else {
                    tvNum.text = it.size.toString()
                    tvAmount.text = it.fold(
                        BigDecimal.ZERO
                    ) { acc, order -> acc + (order.actualRepayAmount ?: BigDecimal.ZERO) }
                        .formatAmount(it[0].currencySymbol)
                }
            }
        }
        togetherRepayResult.observe(this@RepaymentBatchActivity) {
            it?.payUrl?.let { payUrl ->
                WebViewActivity.launch(
                    this@RepaymentBatchActivity,
                    getString(R.string.batch_repayment_orders),
                    payUrl
                )
                finish()
            }
        }
    }
}
