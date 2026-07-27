package com.vaycore.finance.loan.adapter

import androidx.core.view.isVisible
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ProductInstallmentAdapterBinding
import com.vaycore.finance.loan.LoanOfferActivity
import com.vaycore.finance.model.loan.ProductPlanBean
import com.vaycore.finance.util.formatAmountWithPrefix

class InstallmentScheduleAdapter :
    BaseAdapter<ProductPlanBean, ProductInstallmentAdapterBinding>(ProductInstallmentAdapterBinding::inflate) {

    override fun bindItem(
        binding: ProductInstallmentAdapterBinding,
        item: ProductPlanBean,
        position: Int,
    ) = with(binding) {
        tvDueDate.text = item.repayTime?.substringBefore(" ")
        tvAmount.text = item.totalRepayment.formatAmountWithPrefix()
        ivArrow.rotation = 0f
        infoLayout.isVisible = false
        val toggleDetails = {
            infoLayout.isVisible = !infoLayout.isVisible
            ivArrow.rotation = if (!infoLayout.isVisible) 0f else 180f
            if (context is LoanOfferActivity) {
                (context as LoanOfferActivity?)?.scrollToOfferActions()
            }
        }
        tvDueDate.setOnClickListener { toggleDetails() }
        tvAmount.setOnClickListener { toggleDetails() }
        ivArrow.setOnClickListener { toggleDetails() }
        tvLoanAmount.text = item.repayActualAmount.formatAmountWithPrefix()
        tvInterest.text = item.repayInterestAmount.formatAmountWithPrefix()
        tvServiceFee.text = item.repayAfterHandleAmount.formatAmountWithPrefix()
    }
}
