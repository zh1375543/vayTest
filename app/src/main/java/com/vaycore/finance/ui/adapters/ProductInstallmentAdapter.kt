package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.ui.activities.LoanProductActivity
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductPlanBean
import com.vaycore.finance.databinding.ProductInstallmentAdapterBinding
import com.vaycore.finance.util.formatAmount

class ProductInstallmentAdapter :
    BaseAdapter<ProductPlanBean, ProductInstallmentAdapterBinding>(ProductInstallmentAdapterBinding::inflate) {

    override fun bindItem(
        binding: ProductInstallmentAdapterBinding,
        item: ProductPlanBean,
        position: Int,
    ) = with(binding) {
        tvDueDate.text = item.repayTime?.substringBefore(" ")
        tvAmount.text = item.totalRepayment.formatAmount()
        ivArrow.rotation = 0f
        infoLayout.isVisible = false
        val toggleDetails = {
            infoLayout.isVisible = !infoLayout.isVisible
            ivArrow.rotation = if (!infoLayout.isVisible) 0f else 180f
            if (context is LoanProductActivity) {
                (context as LoanProductActivity?)?.scrollBottom()
            }
        }
        tvDueDate.setOnClickListener { toggleDetails() }
        tvAmount.setOnClickListener { toggleDetails() }
        ivArrow.setOnClickListener { toggleDetails() }
        tvLoanAmount.text = item.repayActualAmount.formatAmount()
        tvInterest.text = item.repayInterestAmount.formatAmount()
        tvServiceFee.text = item.repayAfterHandleAmount.formatAmount()
    }
}
