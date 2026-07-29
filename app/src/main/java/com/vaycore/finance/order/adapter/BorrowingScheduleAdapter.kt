package com.vaycore.finance.order.adapter

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.model.loan.ProductPlanBean
import com.vaycore.finance.databinding.OrderInstallmentAdapterBinding
import com.vaycore.finance.order.BorrowingDetailActivity
import com.vaycore.finance.util.context.resolveColorCompat
import com.vaycore.finance.util.formatAmountWithPrefix

class BorrowingScheduleAdapter :
    BaseAdapter<ProductPlanBean, OrderInstallmentAdapterBinding>(OrderInstallmentAdapterBinding::inflate) {

    override fun bindItem(
        binding: OrderInstallmentAdapterBinding,
        item: ProductPlanBean,
        position: Int,
    ) = with(binding) {
        tvDate.text = item.repayTime?.substringBefore(" ")
        tvAmount.text = item.actualNeedRepayAmount.formatAmountWithPrefix()
        tvLoanAmount.text = item.needRepayLoanAmount.formatAmountWithPrefix()
        tvInterest.text = item.needRepayInterestSum.formatAmountWithPrefix()
        tvServiceFee.text = item.needRepayAfterHandleAmount.formatAmountWithPrefix()
        tvDueFee.text = item.needRepayPenaltyAmount.formatAmountWithPrefix()
        detailLayout.isVisible = item.isExpend
        ivArrow.rotation = if (item.isExpend) 180f else 0f
        tvStatus.setOnClickListener { ivArrow.performClick() }
        ivArrow.setOnClickListener {
            detailLayout.isVisible = !detailLayout.isVisible
            ivArrow.rotation = if (detailLayout.isVisible) 180f else 0f
            if (context is BorrowingDetailActivity?) {
                (context as BorrowingDetailActivity?)?.scrollToRepaymentOptions()
            }
            item.isExpend = detailLayout.isVisible
        }
        ivCheck.setImageResource(
            if (item.isSelect) (if (item.isProcess()) R.mipmap.ic_due_select else R.mipmap.ic_due_select)
            else R.mipmap.ic_due_normal
        )
        val isSelect =
            if (position == items.size - 1) item.isSelect else (items[position + 1].isSelect)
        tvStatus.setTextColor(context.resolveColorCompat(R.color.text_body))
        line.isVisible = position != items.size - 1
        dueFeeItem.isVisible = false
        when (item.planStatus) {
            34, 35 -> {
                dueFeeItem.isVisible = true
                tvStatus.text = context.getString(R.string.overdue)
                tvStatus.setTextColor(context.resolveColorCompat(R.color.status_error))
            }

            31 -> {
                tvStatus.text = context.getString(R.string.processing)
                tvStatus.setTextColor(context.resolveColorCompat(R.color.status_success))
            }

            30, 32, 35 -> {
                tvStatus.text = context.getString(R.string.pending)
                tvStatus.setTextColor(context.resolveColorCompat(R.color.status_success))
            }

            40, 41, 42, 43 -> {
                tvStatus.text = context.getString(R.string.settled)
//                    tvDate.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                    tvAmount.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                tvStatus.setTextColor(context.resolveColorCompat(R.color.text_body))
            }

            14, 15 -> {
                tvStatus.text = context.getString(R.string.rejected)
//                    tvDate.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                    tvAmount.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                tvStatus.setTextColor(context.resolveColorCompat(R.color.text_body))
            }

            23 -> {
                tvStatus.text = context.getString(R.string.closed)
//                    tvDate.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                    tvAmount.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                tvStatus.setTextColor(context.resolveColorCompat(R.color.text_body))
            }

            22 -> {
                tvStatus.text = context.getString(R.string.invalid)
//                    tvDate.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                    tvAmount.setTextColor(context.resolveColorCompat(R.color.C_75707E))
//                tvStatus.setTextColor(context.resolveColorCompat(R.color.text_body))
            }
        }
    }

}
