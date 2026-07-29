package com.vaycore.finance.order

import android.os.Build
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.bean.ClickablePart
import com.vaycore.finance.data.bean.TrackBean
import com.vaycore.finance.databinding.ActivityBorrowingDetailBinding
import com.vaycore.finance.data.ACT_inOrdersDetail
import com.vaycore.finance.data.ACT_inRepaymentLink
import com.vaycore.finance.data.LEASE_AGREEMENT
import com.vaycore.finance.data.ORDER_STATUS_AUTO
import com.vaycore.finance.data.ORDER_STATUS_AUTO_FAIL
import com.vaycore.finance.data.ORDER_STATUS_BAD_DEBTS
import com.vaycore.finance.data.ORDER_STATUS_CASH
import com.vaycore.finance.data.ORDER_STATUS_CLOSE
import com.vaycore.finance.data.ORDER_STATUS_INVALID
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.ORDER_STATUS_MANUAL
import com.vaycore.finance.data.ORDER_STATUS_MANUAL_FAIL
import com.vaycore.finance.data.ORDER_STATUS_OVERDUE
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_FAIL
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_ING
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_PROCESS
import com.vaycore.finance.data.ORDER_STATUS_REVIEW
import com.vaycore.finance.data.ORDER_STATUS_SETTLE
import com.vaycore.finance.data.ORDER_STATUS_SETTLE_REDUCE
import com.vaycore.finance.data.ORDER_STATUS_SETTLE_REDUCE_OR_RENEWAL
import com.vaycore.finance.data.ORDER_STATUS_SETTLE_RENEWAL
import com.vaycore.finance.data.ORDER_STATUS_SUCCESS
import com.vaycore.finance.data.PAWN_AGREEMENT
import com.vaycore.finance.data.PageOrderDetail
import com.vaycore.finance.data.PageRepaymentLink
import com.vaycore.finance.model.order.LoanOrderDetailResponse
import com.vaycore.finance.order.adapter.BorrowingFeeBreakdownAdapter
import com.vaycore.finance.order.adapter.BorrowingScheduleAdapter
import com.vaycore.finance.util.LOAN_ORDER_CONFIRMATION_PAGE
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.setSpannableClickableTexts
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.payback.RepaymentSubmissionActivity
import com.vaycore.finance.browser.WebViewActivity
import com.vaycore.finance.ui.views.StatefulActionButton
import com.vaycore.finance.payback.showRepayAndReapplyDialog
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.isPositive
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import java.math.BigDecimal
import kotlin.toString

class BorrowingDetailActivity :
    BaseActivity<ActivityBorrowingDetailBinding>() {

    override val binding by viewBinding(ActivityBorrowingDetailBinding::inflate)
    private val isFromBatch by lazy { intent.getBooleanExtra("isFromBatch", false) }
    private val vm by viewModels<LoanOrderViewModel>()

    private val orderId by lazy { intent.getLongExtra("orderId", 0L) }
    // Server state reloanButtonSign supports "0"-"4"; null/unknown values fall back to state 2.
    private var currentButtonSign: String? = null
    private var isAwaitingBottomActionState = false
    private val feeAdapter by lazy {
        BorrowingFeeBreakdownAdapter()
    }
    private val installAdapter by lazy {
        BorrowingScheduleAdapter().apply {
            setOnItemClickListener { item, position ->
                val lastDueIndex = items.indexOfLast { it1 -> it1.isDue() }
                val firstProcessIndex = items.indexOfFirst { it1 -> it1.isProcess() }
//                if (item.isDueAndSettle()) return@setOnItemClickListener
                items.forEachIndexed { i, t ->
                    t.isSelect = i <= position
                    if (t.isDueAndSettle()) {
                        t.isSelect = true
                    }
                }
//                if (firstProcessIndex >= 0) {
//                    items[firstProcessIndex].isSelect = true
//                }
//                if (lastDueIndex >= 0 && lastDueIndex + 1 < items.size) {
//                    items[lastDueIndex + 1].isSelect = true
//                }
                if (item.isDueAndSettle()) {
                    item.isSelect = true
                }
                notifyItemRangeChanged(0, itemCount, 0)
                binding.tvSelectAmount.text =
                    items.filter { it1 -> !it1.isSettle() && it1.isSelect }
                        .fold(
                            BigDecimal.ZERO
                        ) { acc, order ->
                            acc + (order.actualNeedRepayAmount
                                ?: BigDecimal.ZERO)
                        }
                        .formatAmountWithPrefix(orderDetail?.appOrderInfoDto?.currencySymbol)
            }
        }
    }

    override fun initView() {
        primeOrderDetailSurface()
        connectPaymentEntry()
        connectRenewalControls()
    }

    private fun primeOrderDetailSurface() = with(binding) {
        trackEvent(LOAN_ORDER_CONFIRMATION_PAGE)
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getOrderDetail(orderId) {
                loadingLayout.showError()
            }
        }
        rvFee.adapter = feeAdapter
        contractLayout.setSpannableClickableTexts(
            String.format(
                getString(R.string.product_detail_agreement),
                getString(R.string.lease_contract),
                getString(R.string.mortgage_contract),
            ),
            listOf(
                ClickablePart(
                    getString(R.string.lease_contract),
                    resolveColorCompat(R.color.color_7087F8),
                ) {
                    showLoanAgreement(getString(R.string.lease_contract), LEASE_AGREEMENT)
                },
                ClickablePart(
                    getString(R.string.mortgage_contract),
                    resolveColorCompat(R.color.color_7087F8),
                ) {
                    showLoanAgreement(getString(R.string.mortgage_contract), PAWN_AGREEMENT)
                },
            ),
        )
        rvPlan.adapter = installAdapter
        tvInstallment.singleClick {
            val expanded = !installmentContentGroup.isVisible
            installmentContentGroup.isVisible = expanded
        }
    }

    private fun connectPaymentEntry() = with(binding) {
        tvApply.singleClick {
            val payGoUrl = orderDetail?.appOrderRepayDto?.payGoUrl
            if (payGoUrl.isNullOrBlank()) {
                start<RepaymentSubmissionActivity> {
                    putExtra("orderNo", orderDetail?.appOrderInfoDto?.orderNo ?: "")
                    putExtra("amount", tvTotalRepay.text.toString())
                    putExtra("orderId", orderId.toString())
                }
            } else {
                vm.submitTrackingEvent(
                    TrackBean(
                        p = PageRepaymentLink,
                        act = ACT_inRepaymentLink,
                        result = System.currentTimeMillis().toString()
                    )
                )
                WebViewActivity.launch(
                    this@BorrowingDetailActivity, getString(R.string.repayment), payGoUrl
                )
            }
        }
    }

    private fun connectRenewalControls() = with(binding) {
        tvRepay.singleClick {
            if (installLayout.isVisible && installAdapter.items.none { it1 -> !it1.isSettle() && it1.isSelect }) {
                getString(R.string.toast_repayment_select).showToastMessage()
                return@singleClick
            }
            if (currentButtonSign == "2" || currentButtonSign == "3"|| currentButtonSign == "4") {
                vm.cancelApply(orderId) {
                    proceedWithSelectedRepayment()
                }
            } else {
                proceedWithSelectedRepayment()
            }
        }
        tvBorrow.singleClick {
            if (shouldBlockUncheckedAgreement()) {
                getString(R.string.toast_repay_auto_apply_agreement).showToastMessage()
                return@singleClick
            }
            if (installLayout.isVisible && installAdapter.items.none { it1 -> !it1.isSettle() && it1.isSelect }) {
                getString(R.string.toast_repayment_select).showToastMessage()
                return@singleClick
            }
            if (currentButtonSign == "1") {
                vm.repayAndBorrow(orderId, 1) {
                    proceedWithSelectedRepayment()
                }
            } else if (tvBorrow.text.toString() == getString(R.string.repay)) {
                tvRepay.performClick()
            } else {
                showRepayAndReapplyDialog(
                    isDue = tvBorrow.isSelected,
                    closeAction = {
                        cbAutoApply.isSelected = false
                        refreshAutoApplyButtons()
                    },
                    confirmAction = {
                        vm.repayAndBorrow(orderId, 1) {
                            proceedWithSelectedRepayment()
                        }
                    }
                )
            }
        }
        tvBorrowAll.singleClick {
            showRepayAndReapplyDialog(
                isDue = tvBorrow.isSelected,
                isApplyAll = true,
                closeAction = {
                    cbAutoApply.isSelected = false
                    refreshAutoApplyButtons()
                },
                confirmAction = {
                    vm.repayAndBorrow(orderId, 2) {
                        proceedWithSelectedRepayment()
                    }
                },
            )
        }
        cbAutoApply.setOnClickListener {
            cbAutoApply.isSelected = !cbAutoApply.isSelected
            refreshAutoApplyButtons()
        }
        tvPrivacy.setOnClickListener {
            cbAutoApply.isSelected = !cbAutoApply.isSelected
            refreshAutoApplyButtons()
        }
    }

    private fun proceedWithSelectedRepayment() = with(binding) {
        if (installLayout.isVisible) {
            vm.installmentRepay(
                orderNo = orderDetail?.appOrderInfoDto?.orderNo,
                planNumberList = installAdapter.items
                    .filter { item -> !item.isSettle() && item.isSelect }
                    .map { it.planPart },
            )
        } else {
            tvApply.performClick()
        }
    }

    /** States 3/4 switch between plain repayment and auto-apply actions via the checkbox. */
    private fun refreshAutoApplyButtons() = with(binding) {
        if (currentButtonSign == "3" || currentButtonSign == "4") {
            tvBorrow.text = getString(
                if (cbAutoApply.isSelected) R.string.repay_auto_apply else R.string.repay
            )
            tvBorrowAll.isVisible = currentButtonSign == "4" && cbAutoApply.isSelected
            tvBorrowTip.isVisible = currentButtonSign == "3" && cbAutoApply.isSelected
            tvBorrowAllTip.isVisible = currentButtonSign == "4" && cbAutoApply.isSelected
        }
    }

    override fun onResume() {
        super.onResume()
        binding.loadingLayout.showLoading()
        vm.getOrderDetail(orderId) {
            binding.loadingLayout.showError()
        }
    }

    private fun showLoanAgreement(title: String, baseUrl: String) {
        WebViewActivity.launch(
            this,
            title,
            baseUrl + "userId=${orderDetail?.appOrderInfoDto?.userId}&productId=${orderDetail?.appOrderInfoDto?.productId}&amount=${orderDetail?.appOrderInfoDto?.loanAmount.toString()}"
        )
    }

    private var orderDetail: LoanOrderDetailResponse? = null
    private fun renderOrderDetail(detail: LoanOrderDetailResponse) = with(binding) {
        orderDetail = detail
        loadingLayout.showContent()
        installGroup.isVisible = false
        val plans = detail.installmentRepaymentPlanDTOList.orEmpty()
        val lastDueIndex = plans.indexOfLast { it.isDueAndSettle() }
        val firstProcessIndex = plans.indexOfFirst { it.isProcess() }
        installAdapter.submitItems(plans.onEachIndexed { index, it1 ->
//                            it1.planStatus =34
            it1.isSelect =
                (index == lastDueIndex + 1) || index == firstProcessIndex
            if (it1.isDueAndSettle()) {
                it1.isSelect = true
            }
            it1.isExpend = it1.isSelect
        })
        val hasInstallments = plans.isNotEmpty()
        detailLayout.isVisible = hasInstallments
        installmentTipLayout.isVisible = hasInstallments
        installLayout.isVisible = hasInstallments
        installmentContentGroup.isVisible = hasInstallments
        detail.appOrderInfoDto?.let { order ->
            tvLoanAmount.text =
                String.format(getString(R.string.loan_amount), order.currency)
            tvAmount.text =
                order.loanAmount.formatAmountWithPrefix(order.currencySymbol)
            tvModel.text = "${Build.BRAND} ${Build.MODEL}"
            tvProductName.text = order.productName
            tvOrderNo.text = order.orderNo
            tvOrderStatus.setTextColor(resolveColorCompat(R.color.color_7087F8))
            when (order.status) {
                ORDER_STATUS_SUCCESS,
                ORDER_STATUS_REVIEW,
                ORDER_STATUS_AUTO,
                ORDER_STATUS_MANUAL,
                ORDER_STATUS_CASH,
                ORDER_STATUS_PAYMENT_ING,
                ORDER_STATUS_PAYMENT_FAIL,
                    -> {
                    installLayout.isVisible = false
                    installmentContentGroup.isVisible = false
                    installmentTipLayout.isVisible = false
                    detailLayout.isVisible = false
                    tvOrderStatus.text = getString(R.string.pending_cash)
                }

                ORDER_STATUS_PAYMENT_PROCESS -> {
                    tvOrderStatus.text =
                        getString(R.string.repayment_processing)
                }

                ORDER_STATUS_PAYMENT_PENDING,
                ORDER_STATUS_IN_RENEWAL,
                ORDER_STATUS_IN_RENEWAL_PROCESS,
                    -> {
                    tvOrderStatus.text = getString(R.string.pending_repayment)
                }

                ORDER_STATUS_OVERDUE,
                ORDER_STATUS_BAD_DEBTS,
                    -> {
                    tvOrderStatus.text = getString(R.string.overdue)
                    tvOrderStatus.setTextColor(resolveColorCompat(R.color.status_error))
                }

                ORDER_STATUS_AUTO_FAIL,
                ORDER_STATUS_MANUAL_FAIL,
                    -> {
                    tvOrderStatus.text = getString(R.string.reject)
                }

                ORDER_STATUS_CLOSE,
                ORDER_STATUS_INVALID,
                    -> {
                    tvOrderStatus.text = getString(R.string.closed)
                }

                ORDER_STATUS_SETTLE,
                ORDER_STATUS_SETTLE_REDUCE,
                ORDER_STATUS_SETTLE_RENEWAL,
                ORDER_STATUS_SETTLE_REDUCE_OR_RENEWAL,
                    -> {
                    tvOrderStatus.text = getString(R.string.complete)
                }
            }
            tvOrderStatusTop.text = tvOrderStatus.text
            vm.submitTrackingEvent(
                TrackBean(
                    p = PageOrderDetail,
                    act = ACT_inOrdersDetail,
                    result = System.currentTimeMillis()
                        .toString() + "|" + order.orderId + "|" + order.status
                )
            )
            tvApplyDate.text = detail.applyDateStr ?: "-"
            tvLoanDate.text = detail.loanDateStr ?: "-"
            tvDueDate.text = detail.shouldRepayDateStr ?: "-"
            tvDays.text =
                String.format(
                    getString(R.string.num_days),
                    order.timeLimit.toString()
                )

            tvInterestTitle.text =
                String.format(
                    getString(R.string.interest_day),
                    detail.dayRateStr + "%"
                )
            tvInterest.text =
                detail.interestAmount.formatAmountWithPrefix(order.currencySymbol)
            tvActuallyAmount.text =
                detail.actualAmount.formatAmountWithPrefix(order.currencySymbol)
            tvInstallFee.text =
                detail.totalInstallmentServiceFee.formatAmountWithPrefix(order.currencySymbol)
            installGroup.isVisible = detail.totalInstallmentServiceFee.isPositive()
            tvAccount.text = detail.bankNo
            feeAdapter.currencySymbol = order.currencySymbol
            feeAdapter.submitItems(order.orderHandleFees)
            tvTotalRepayTitle.text =
                String.format(
                    getString(R.string.total_repayment_str),
                    order.currency
                )
            tvTotalRepay.text =
                detail.actualNeedRepayAmount.formatAmountWithPrefix(order.currencySymbol)
            val isDue =
                order.status == ORDER_STATUS_OVERDUE || order.status == ORDER_STATUS_BAD_DEBTS
            tvDueFee.isVisible = isDue
            tvDueFeeTitle.isVisible = isDue
            tvDueFee.text =
                detail.appOrderRepayDto?.penaltyAmount.formatAmountWithPrefix(
                    null
                )
            tvTotalRepay.isSelected = isDue
            tvOrderStatusTop.isSelected = isDue
            tvApply.isSelected = isDue
            tvBorrow.isSelected = isDue
            tvRepay.isSelected = isDue
            updateBottomActionColors(isDue)
            hideBottomActionPanel()
            when (order.status) {
                ORDER_STATUS_PAYMENT_PENDING,
                ORDER_STATUS_IN_RENEWAL,
                ORDER_STATUS_IN_RENEWAL_PROCESS,
                ORDER_STATUS_OVERDUE,
                ORDER_STATUS_BAD_DEBTS,
                    -> {
                    if (!isFromBatch) {
                        val selectedAmount =
                            if (installLayout.isVisible) {
                                installAdapter.items.filter { it1 -> !it1.isSettle() && it1.isSelect }
                                    .fold(
                                        BigDecimal.ZERO
                                    ) { acc, order ->
                                        acc + (order.actualNeedRepayAmount
                                            ?: BigDecimal.ZERO)
                                    }
                                    .formatAmountWithPrefix(order.currencySymbol)
                            } else {
                                detail.actualNeedRepayAmount.formatAmountWithPrefix(order.currencySymbol)
                            }
                        presentBorrowingActions(
                            selectedAmount = selectedAmount
                        )
                        vm.getButtonState()
                    }
                }

                else -> {
                    tvApply.isVisible = false
                }
            }
        }
    }

    override fun initObserve() = with(vm) {
        super.initObserve()

        orderDetailResult.observe(this@BorrowingDetailActivity) { detail ->
            detail?.let(::renderOrderDetail)
        }
        buttonResult.observe(this@BorrowingDetailActivity) { sign ->
            if (isAwaitingBottomActionState) {
                renderRepaymentActionState(sign)
                binding.bottomActionLayout.visibility = View.VISIBLE
                isAwaitingBottomActionState = false
            }
        }
        installmentRepayResult.observe(this@BorrowingDetailActivity) {
            openRepaymentPage(it?.payUrl)
        }
    }

    private fun openRepaymentPage(payUrl: String?) {
        if (!payUrl.isNullOrBlank()) {
            WebViewActivity.launch(this, getString(R.string.repayment), payUrl)
            return
        }
        start<RepaymentSubmissionActivity> {
            putExtra("orderNo", orderDetail?.appOrderInfoDto?.orderNo.orEmpty())
            putExtra("amount", binding.tvTotalRepay.text.toString())
            putExtra("orderId", orderId.toString())
        }
    }

    fun scrollToRepaymentOptions() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun hideBottomActionPanel() {
        currentButtonSign = null
        isAwaitingBottomActionState = false
        binding.bottomActionLayout.isVisible = false
    }

    private fun presentBorrowingActions(
        selectedAmount: String,
    ) = with(binding) {
        currentButtonSign = null
        isAwaitingBottomActionState = true
        // Keep the bottom area reserved until the server state is known, avoiding a default-state flash.
        bottomActionLayout.visibility = View.INVISIBLE
        cbAutoApply.isSelected = true
        tvSelectAmount.text = selectedAmount
    }

    /** Applies state 0-4 to the fixed bottom layout without rearranging constraints. */
    private fun renderRepaymentActionState(sign: String?) = with(binding) {
        val normalizedSign = when (sign) {
            "0", "1", "2", "3", "4" -> sign
            else -> "2"
        }
        currentButtonSign = normalizedSign
        syncBottomVisibility(normalizedSign)
        when (normalizedSign) {
            "0" -> tvRepay.text = getString(R.string.repay)
            "1" -> {
                tvRepay.text = getString(R.string.repay)
                tvBorrow.text = getString(R.string.repay_auto_apply)
            }
            "3", "4" -> refreshAutoApplyButtons()
            else -> {
                tvRepay.text = getString(R.string.repay)
                tvBorrow.text = getString(R.string.repay_auto_apply)
            }
        }
    }

    private fun syncBottomVisibility(sign: String?) = with(binding) {
        val isState0 = sign == "0"
        val isState1 = sign == "1"
        val isState3 = sign == "3"
        val isState4 = sign == "4"
        val isState2 = !isState0 && !isState1 && !isState3 && !isState4
        tvSelect.isVisible = true
        tvSelectAmount.isVisible = true
        tvRepay.isVisible = isState0 || isState1 || isState2
        cbAutoApply.isVisible = isState2 || isState3 || isState4
        tvPrivacy.isVisible = isState2 || isState3 || isState4
        tvBorrow.isVisible = !isState0
        tvBorrowAll.isVisible = isState4 && cbAutoApply.isSelected
        tvBorrowTip.isVisible = isState2 || (isState3 && cbAutoApply.isSelected)
        tvBorrowAllTip.isVisible = isState4 && cbAutoApply.isSelected
    }

    // Only state 2 blocks Auto-Apply while the agreement is unchecked.
    private fun shouldBlockUncheckedAgreement(): Boolean =
        currentButtonSign != "1" && currentButtonSign != "3" && currentButtonSign != "4" &&
            binding.cbAutoApply.isVisible && !binding.cbAutoApply.isSelected

    private fun updateBottomActionColors(isDue: Boolean) = with(binding) {
        val actionColor = resolveColorCompat(if (isDue) R.color.status_error else R.color.color_7087F8)
        tvRepay.updateAppearance(
            variant = StatefulActionButton.VARIANT_OUTLINE,
            strokeColor = actionColor,
            textColor = actionColor,
        )
        tvBorrow.updateAppearance(
            variant = StatefulActionButton.VARIANT_FILLED,
            solidColor = actionColor,
            textColor = resolveColorCompat(R.color.white),
        )
    }
}
