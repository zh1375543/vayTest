package com.vaycore.finance.payback

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.RepaymentBatchActivityBinding
import com.vaycore.finance.order.BorrowingDetailActivity
import com.vaycore.finance.payback.adapter.BatchRepaymentAdapter
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.start
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.web.WebViewActivity
import com.vaycore.finance.util.viewBinding

class BulkRepaymentActivity :
    BaseActivity<RepaymentBatchActivityBinding>() {

    override val binding by viewBinding(RepaymentBatchActivityBinding::inflate)
    private val vm by viewModels<RepayViewModel>()

    private val orderAdapter by lazy {
        BatchRepaymentAdapter().apply {
            setOnItemClickListener { item, position ->
                item.isCheck = !item.isCheck
                notifyItemRangeChanged(position, 1, 0)
                vm.updateBatchSelection(items)
            }
            setOnChildClickListener { view, item, _ ->
                if (view.id == R.id.tvProductDetail) {
                    start<BorrowingDetailActivity> {
                        putExtra("orderId", item.orderId)
                        putExtra("isFromBatch", true)
                    }
                }
            }
        }
    }

    override fun initView() = with(binding) {
        viewModel = vm
        rvOrder.adapter = orderAdapter
        tvApply.singleClick {
            if (orderAdapter.items.none { it1 -> it1.isCheck }) {
                getString(R.string.toast_empty_choose_repayment).showToastMessage()
                return@singleClick
            }
            vm.togetherRepayment(
                orderAdapter.items
                    .filter { it.isCheck }
                    .mapNotNull { it.orderNo }
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
        orderListResult.observe(this@BulkRepaymentActivity) {
            binding.apply {
                loadingLayout.showContent()
            }
        }
        togetherRepayResult.observe(this@BulkRepaymentActivity) {
            it?.payUrl?.let { payUrl ->
                WebViewActivity.launch(
                    this@BulkRepaymentActivity,
                    getString(R.string.batch_repayment_orders),
                    payUrl
                )
                finish()
            }
        }
    }
}
