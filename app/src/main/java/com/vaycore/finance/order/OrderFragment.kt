package com.vaycore.finance.order

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.ACT_inOrdersPage
import com.vaycore.finance.data.PageOrder
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.ORDER_STATUS_OVERDUE
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.databinding.FragmentBorrowingOverviewBinding
import com.vaycore.finance.loan.viewmodel.LoanDashboardViewModel
import com.vaycore.finance.payback.BulkRepaymentActivity
import com.vaycore.finance.order.adapter.HomeOrderAdapter
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class OrderFragment : BaseFragment<FragmentBorrowingOverviewBinding>(R.layout.fragment_borrowing_overview) {
    override val binding by viewBinding(FragmentBorrowingOverviewBinding::bind)
    private val vm by viewModels<LoanDashboardViewModel>()

    private val orderAdapter by lazy {
        HomeOrderAdapter().apply {
            setOnItemClickListener { item, _ ->
                context.start<BorrowingDetailActivity> {
                    putExtra("orderId", item.orderId)
                    putExtra("isFromBatch", false)
                }
            }
        }
    }

    override fun initView() = with(binding) {
        rvOrder.adapter = orderAdapter
        marqueeView.setTexts(isWhiteColor = false)
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getAuthData()
        }
        tvRepayment.singleClick {
            it.context.start<BulkRepaymentActivity>()
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
        vm.submitTrackingEvent(
            TrackBean(
                p = PageOrder,
                act = ACT_inOrdersPage,
                result = System.currentTimeMillis().toString()
            )
        )
    }

    override fun initObserve() =with(vm){
        loadFailedResult.observe(this@OrderFragment) {
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
                            root.context.resolveColorCompat(R.color.brand_primary)
                        )
                        repaymentLayout.isVisible = it.showMultipleRepaySign == 1 && size > 0
                    }
                }
            }
        }
    }
}
