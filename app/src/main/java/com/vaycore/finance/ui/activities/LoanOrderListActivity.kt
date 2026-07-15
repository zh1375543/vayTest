package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.OrderListAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.databinding.ActivityLoanOrderListBinding
import com.vaycore.finance.data.ACT_inOrderHistory
import com.vaycore.finance.data.PageHistory
import com.vaycore.finance.ui.viewmodels.LoanOrderViewModel
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class LoanOrderListActivity : BaseActivity<ActivityLoanOrderListBinding>() {

    override val binding by viewBinding(ActivityLoanOrderListBinding::inflate)
    private val vm by viewModels<LoanOrderViewModel>()

    private val orderAdapter by lazy {
        OrderListAdapter().apply {
            setOnItemClickListener { _, position ->
                start<LoanOrderDetailActivity> {
                    putExtra("orderId", items[position].id)
                    putExtra("isFromBatch", false)
                }
            }
        }
    }

    override fun initView() = with(binding) {
        vm.recordEvent(
            TrackBean(
                p = PageHistory,
                act = ACT_inOrderHistory,
                result = System.currentTimeMillis().toString()
            )
        )
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getOrderList {
                loadingLayout.showError()
            }
        }
        rvOrder.adapter = orderAdapter
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getOrderList {
            binding.loadingLayout.showError()
        }
    }

    override fun initObserve() =with(vm){
        super.initObserve()
        orderListResult.observe(this@LoanOrderListActivity) {
            binding.apply {
                orderAdapter.submitItems(it)
                if (it.isNullOrEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_order_list_null, R.string.order_empty)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
