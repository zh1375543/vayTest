package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.ORDER_STATUS_AUTO
import com.vaycore.finance.data.local.ORDER_STATUS_BAD_DEBTS
import com.vaycore.finance.data.local.ORDER_STATUS_CASH
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.local.ORDER_STATUS_MANUAL
import com.vaycore.finance.data.local.ORDER_STATUS_OVERDUE
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_FAIL
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_ING
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_PROCESS
import com.vaycore.finance.data.local.ORDER_STATUS_REVIEW
import com.vaycore.finance.data.local.ORDER_STATUS_SUCCESS
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.HomeOrderAdapterBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class HomeOrderAdapter :
    BaseAdapter<ProductBean, HomeOrderAdapterBinding>(HomeOrderAdapterBinding::inflate) {

    override fun bindItem(
        binding: HomeOrderAdapterBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        tvRepay.isSelected = false
        tvState.isSelected = false
        tvDesc.isSelected = false
        tvName.text = item.productName
        when (item.orderStatus) {
            ORDER_STATUS_SUCCESS,
            ORDER_STATUS_REVIEW,
            ORDER_STATUS_AUTO,
            ORDER_STATUS_MANUAL,
            ORDER_STATUS_CASH,
            ORDER_STATUS_PAYMENT_ING,
            ORDER_STATUS_PAYMENT_FAIL,
                -> {
                tvState.text = context.getString(R.string.pending_cash)
                tvDesc.text = context.getString(R.string.pending_cash_desc)
                tvRepay.isVisible = false
                tvAmountTitle.text = context.getString(R.string.l_amount)
                tvLoanAmount.text = item.loanAmount.formatAmountWithPrefix(item.currencySymbol)
                tvDateTitle.text = context.getString(R.string.apply_date)
                tvDate.text = item.applyDateStr
            }

            ORDER_STATUS_PAYMENT_PROCESS -> {
                tvState.text = context.getString(R.string.repayment_processing)
                tvDesc.text = context.getString(R.string.pending_repayment_desc)
                tvRepay.isVisible = false
                tvAmountTitle.text = context.getString(R.string.total_repayment)
                tvLoanAmount.text = item.actualRepayAmount.formatAmountWithPrefix(item.currencySymbol)
                tvDateTitle.text = context.getString(R.string.due_date)
                tvDate.text = item.repayTimeStr
            }

            ORDER_STATUS_PAYMENT_PENDING,
            ORDER_STATUS_IN_RENEWAL,
            ORDER_STATUS_IN_RENEWAL_PROCESS,
                -> {
                tvState.text = context.getString(R.string.pending_repayment)
                tvDesc.text = context.getString(R.string.pending_repayment_desc)
                tvRepay.isVisible = true
                tvAmountTitle.text = context.getString(R.string.total_repayment)
                tvLoanAmount.text = item.actualRepayAmount.formatAmountWithPrefix(item.currencySymbol)
                tvDateTitle.text = context.getString(R.string.due_date)
                tvDate.text = item.repayTimeStr
            }

            ORDER_STATUS_OVERDUE,
            ORDER_STATUS_BAD_DEBTS,
                -> {
                tvState.isSelected = true
                tvDesc.isSelected = true
                tvRepay.isSelected = true
                tvState.text = context.getString(R.string.overdue)
                tvDesc.text = context.getString(R.string.overdue_desc)
                tvRepay.isVisible = true
                tvAmountTitle.text = context.getString(R.string.total_repayment)
                tvLoanAmount.text = item.actualRepayAmount.formatAmountWithPrefix(item.currencySymbol)
                tvDateTitle.text = context.getString(R.string.due_date)
                tvDate.text = item.repayTimeStr
            }
        }
        tvAmountTitle.text = buildString {
            append(tvAmountTitle.text.toString())
            append(":")
        }
        tvDateTitle.text = buildString {
            append(tvDateTitle.text.toString())
            append(":")
        }
    }
}