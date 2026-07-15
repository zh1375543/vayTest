package com.vaycore.finance.ui.fragments

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.ui.activities.RepaymentBatchActivity
import com.vaycore.finance.ui.activities.LoanOrderDetailActivity
import com.vaycore.finance.ui.adapters.HomeOrderAdapter
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.local.ORDER_STATUS_OVERDUE
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.databinding.OrderFragmentBinding
import com.vaycore.finance.data.ACT_inOrdersPage
import com.vaycore.finance.data.PageOrder
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class OrderFragment : BaseFragment<OrderFragmentBinding>(R.layout.order_fragment) {
    override val binding by viewBinding(OrderFragmentBinding::bind)
    private val vm by viewModels<DashboardViewModel>()

    private val orderAdapter by lazy {
        HomeOrderAdapter().apply {
            setOnItemClickListener { item, _ ->
                context.start<LoanOrderDetailActivity> {
                    putExtra("orderId", item.orderId)
                    putExtra("isFromBatch", false)
                }
            }
        }
    }

    override fun initView() = with(binding) {
        rvOrder.adapter = orderAdapter
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getAuthData()
        }
        tvRepayment.singleClick {
            it.context.start<RepaymentBatchActivity>()
        }
        swipeRefreshLayout.setOnRefreshListener {
            binding.loadingLayout.showLoading()
            vm.getAuthData()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getAuthData()
        vm.recordEvent(
            TrackBean(
                p = PageOrder,
                act = ACT_inOrdersPage,
                result = System.currentTimeMillis().toString()
            )
        )
    }

    override fun initObserve() =with(vm){
        authFailedResult.observe(this@OrderFragment) {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.loadingLayout.showError()
        }
        authResult.observe(this@OrderFragment) {
            it?.let {
                binding.apply {
                    swipeRefreshLayout.isRefreshing = false
                    it.repayProducts?.let { orderList ->
                        loadingLayout.showContent()
                        emptyOrder.isVisible = orderList.isEmpty()
                        orderLayout.isVisible = orderList.isNotEmpty()
                        orderAdapter.submitItems(orderList)
                        val size = orderList.filter { it1 ->
                            it1.orderStatus ==
                                    ORDER_STATUS_PAYMENT_PENDING
                                    || it1.orderStatus == ORDER_STATUS_IN_RENEWAL
                                    || it1.orderStatus ==
                                    ORDER_STATUS_IN_RENEWAL_PROCESS
                                    || it1.orderStatus == ORDER_STATUS_OVERDUE
                        }.size
                        tvOrderNum.setClickableTextWithScale(
                            String.format(
                                getString(R.string.home_order_num),
                                size.toString()
                            ),
                            size.toString(),
                            root.context.getColor2(R.color.white)
                        )
                        repaymentLayout.isVisible = it.showMultipleRepaySign == 1 && size > 0
                    }
                }
            }
        }
    }
}
