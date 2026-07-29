package com.vaycore.finance.order.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
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
import com.vaycore.finance.databinding.ItemOrderBinding
import com.vaycore.finance.model.order.OrderBean
import com.vaycore.finance.ui.binding.BindableItemsAdapter
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.formatAmountWithPrefix

class BorrowingHistoryAdapter :
    BaseAdapter<OrderBean, ItemOrderBinding>(ItemOrderBinding::inflate), BindableItemsAdapter {

    override fun submitBindingItems(items: List<*>?) {
        submitItems(items.orEmpty().filterIsInstance<OrderBean>())
    }

    override fun bindItem(
        binding: ItemOrderBinding,
        item: OrderBean,
        position: Int,
    ) {
        binding.item = item
        binding.tvLoanAmount.text = binding.root.context.getString(R.string.loan_amount, item.currency)
        binding.tvDays.text = binding.root.context.getString(R.string.num_days, item.timeLimit.toString())
        binding.tvAmount.text = item.loanAmount.formatAmountWithPrefix(item.currencySymbol)
        binding.tvDateTitle.text = binding.root.context.getString(R.string.apply_date)
        binding.tvDate.text = item.createTime?.substringBefore(' ')
        binding.tvStatus.setTextColor(binding.root.context.resolveColorCompat(R.color.brand_primary))
        binding.tvStatus.text = when (item.status) {
            ORDER_STATUS_SETTLE,
            ORDER_STATUS_SETTLE_REDUCE,
            ORDER_STATUS_SETTLE_RENEWAL,
            ORDER_STATUS_SETTLE_REDUCE_OR_RENEWAL -> binding.root.context.getString(R.string.complete)

            ORDER_STATUS_SUCCESS,
            ORDER_STATUS_REVIEW,
            ORDER_STATUS_AUTO,
            ORDER_STATUS_MANUAL,
            ORDER_STATUS_CASH,
            ORDER_STATUS_PAYMENT_ING,
            ORDER_STATUS_PAYMENT_FAIL -> binding.root.context.getString(R.string.pending_cash)

            ORDER_STATUS_AUTO_FAIL,
            ORDER_STATUS_MANUAL_FAIL -> binding.root.context.getString(R.string.reject)

            ORDER_STATUS_CLOSE,
            ORDER_STATUS_INVALID -> binding.root.context.getString(R.string.closed)

            ORDER_STATUS_PAYMENT_PROCESS -> binding.root.context.getString(R.string.repayment_processing)

            ORDER_STATUS_PAYMENT_PENDING,
            ORDER_STATUS_IN_RENEWAL,
            ORDER_STATUS_IN_RENEWAL_PROCESS -> binding.root.context.getString(R.string.pending_repayment)

            ORDER_STATUS_OVERDUE,
            ORDER_STATUS_BAD_DEBTS -> {
                binding.tvStatus.setTextColor(binding.root.context.resolveColorCompat(R.color.status_error))
                binding.root.context.getString(R.string.overdue)
            }

            else -> binding.root.context.getString(R.string.pending_repayment)
        }
        binding.ivArrow.imageTintList = binding.tvStatus.textColors
        binding.executePendingBindings()
    }
}
