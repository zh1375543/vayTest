package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.ui.activities.LoanOrderDetailActivity
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
import com.vaycore.finance.databinding.BatchRepaymentAdapterBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.formatAmountWithPrefix

class BatchRepaymentAdapter :
    BaseAdapter<ProductBean, BatchRepaymentAdapterBinding>(BatchRepaymentAdapterBinding::inflate) {

    override fun bindItem(
        binding: BatchRepaymentAdapterBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        ivCheck.isSelected = item.isCheck
        tvName.text = item.productName
        tvAmount.text = item.actualRepayAmount.formatAmountWithPrefix(item.currencySymbol)
        tvProductDetail.singleClick {
            context.start<LoanOrderDetailActivity> {
                putExtra("orderId", item.orderId)
                putExtra("isFromBatch", true)
            }
        }
        tvStatus.isSelected = false
        when (item.orderStatus) {
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
                tvStatus.isSelected = true
                tvStatus.text = context.getString(R.string.overdue)
            }
        }
    }
}
