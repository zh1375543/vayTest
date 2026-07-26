package com.vaycore.finance.repayment.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.ORDER_STATUS_AUTO
import com.vaycore.finance.data.ORDER_STATUS_BAD_DEBTS
import com.vaycore.finance.data.ORDER_STATUS_CASH
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.ORDER_STATUS_MANUAL
import com.vaycore.finance.data.ORDER_STATUS_OVERDUE
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_FAIL
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_ING
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.ORDER_STATUS_PAYMENT_PROCESS
import com.vaycore.finance.data.ORDER_STATUS_REVIEW
import com.vaycore.finance.data.ORDER_STATUS_SUCCESS
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.databinding.BatchRepaymentAdapterBinding
import com.vaycore.finance.ui.binding.BindableItemsAdapter
import com.vaycore.finance.util.formatAmountWithPrefix

class BatchRepaymentAdapter :
    BaseAdapter<ProductBean, BatchRepaymentAdapterBinding>(BatchRepaymentAdapterBinding::inflate),
    BindableItemsAdapter {

    override fun submitBindingItems(items: List<*>?) {
        submitItems(items.orEmpty().filterIsInstance<ProductBean>())
    }

    override fun bindItem(
        binding: BatchRepaymentAdapterBinding,
        item: ProductBean,
        position: Int,
    ) {
        binding.item = item
        binding.tvAmount.text = item.actualRepayAmount.formatAmountWithPrefix(item.currencySymbol)
        binding.tvStatus.isSelected = false
        binding.tvStatus.text = when (item.orderStatus) {
            ORDER_STATUS_SUCCESS,
            ORDER_STATUS_REVIEW,
            ORDER_STATUS_AUTO,
            ORDER_STATUS_MANUAL,
            ORDER_STATUS_CASH,
            ORDER_STATUS_PAYMENT_ING,
            ORDER_STATUS_PAYMENT_FAIL -> binding.root.context.getString(R.string.pending_cash)

            ORDER_STATUS_PAYMENT_PROCESS -> binding.root.context.getString(R.string.repayment_processing)

            ORDER_STATUS_PAYMENT_PENDING,
            ORDER_STATUS_IN_RENEWAL,
            ORDER_STATUS_IN_RENEWAL_PROCESS -> binding.root.context.getString(R.string.pending_repayment)

            ORDER_STATUS_OVERDUE,
            ORDER_STATUS_BAD_DEBTS -> {
                binding.tvStatus.isSelected = true
                binding.root.context.getString(R.string.overdue)
            }

            else -> binding.root.context.getString(R.string.overdue)
        }
        binding.executePendingBindings()
    }

    override fun bindChildClickListeners(
        binding: BatchRepaymentAdapterBinding,
        item: ProductBean,
        position: Int,
    ) {
        super.bindChildClickListeners(binding, item, position)
        binding.tvProductDetail.setOnClickListener {
            dispatchChildClick(it, item, position)
        }
    }
}
