package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.*
import com.vaycore.finance.data.local.bean.OrderBean
import com.vaycore.finance.databinding.ItemOrderBinding
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.context.getColor2

class OrderListAdapter :
    BaseAdapter<OrderBean, ItemOrderBinding>(ItemOrderBinding::inflate) {

    override fun bindItem(
        binding: ItemOrderBinding,
        item: OrderBean,
        position: Int,
    ) = with(binding) {
        tvLoanAmount.text =
            String.format(context.getString(R.string.loan_amount), item.currency)
        tvDays.text =
            String.format(context.getString(R.string.num_days), item.timeLimit.toString())
        tvAmount.text = item.loanAmount.formatAmountWithPrefix(item.currencySymbol)
//                tvDate.text = it
        tvDateTitle.text = context.getString(R.string.apply_date)
        tvDate.text = item.createTime?.split(" ")?.first()
        tvStatus.setTextColor(context.getColor2(R.color.color_7087F8))
        tvProductName.text = item.productName
        when (item.status) {
            ORDER_STATUS_SETTLE,
            ORDER_STATUS_SETTLE_REDUCE,
            ORDER_STATUS_SETTLE_RENEWAL,
            ORDER_STATUS_SETTLE_REDUCE_OR_RENEWAL,
                -> {
                tvStatus.text = context.getString(R.string.complete)
            }

            ORDER_STATUS_SUCCESS,
            ORDER_STATUS_REVIEW,
            ORDER_STATUS_AUTO,
            ORDER_STATUS_MANUAL,
            ORDER_STATUS_CASH,
            ORDER_STATUS_PAYMENT_ING,
            ORDER_STATUS_PAYMENT_FAIL,
                -> {
                tvStatus.text = context.getString(R.string.pending_cash)
            }

            ORDER_STATUS_AUTO_FAIL,
            ORDER_STATUS_MANUAL_FAIL,
                -> {
                tvStatus.text = context.getString(R.string.reject)
            }

            ORDER_STATUS_CLOSE,
            ORDER_STATUS_INVALID,
                -> {
                tvStatus.text = context.getString(R.string.closed)
            }

            ORDER_STATUS_PAYMENT_PROCESS -> {
                tvStatus.text = context.getString(R.string.repayment_processing)
            }

            ORDER_STATUS_PAYMENT_PENDING,
            ORDER_STATUS_IN_RENEWAL,
            ORDER_STATUS_IN_RENEWAL_PROCESS,
                -> {
                tvStatus.text = context.getString(R.string.pending_repayment)
            }

            ORDER_STATUS_OVERDUE,
            ORDER_STATUS_BAD_DEBTS,
                -> {
                tvStatus.setTextColor(context.getColor2(R.color.C_F62909))
                tvStatus.text = context.getString(R.string.overdue)
            }
        }
        ivArrow.imageTintList = tvStatus.textColors
    }
}
