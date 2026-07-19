package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ProductRepaymentMenuAdapterBinding
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.util.formatAmount

class ProductRepaymentMenuAdapter(var selectPosition: Int = 0) :
    BaseAdapter<ProductBean, ProductRepaymentMenuAdapterBinding>(ProductRepaymentMenuAdapterBinding::inflate) {

    override fun bindItem(
        binding: ProductRepaymentMenuAdapterBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        contentView.isSelected = selectPosition == position
        val isInstall = !item.productInstallmentPlanDTOList.isNullOrEmpty()
        val size = if (isInstall) {
            val list = item.productInstallmentPlanDTOList
            val index = list?.indexOfFirst { it.isDefault == 1 || it.defaultSign == 1 }?.coerceIn(0, list.lastIndex) ?: 0
            list?.get(index)?.appRepaymentPlanDTOList?.size ?: 0
        } else 1
        tvNum1.text = size.toString()
        tvNoInstall.isVisible = !isInstall
        firstRepaymentContainer.isVisible = isInstall
        tvFirst.isVisible = isInstall
        tvAmount1.isVisible = isInstall
        tvPeriodDays1.text = item.timeLimit.toString() + context.getString(R.string.days)
        if (isInstall) {
            val list = item.productInstallmentPlanDTOList
            val index = list?.indexOfFirst { it.isDefault == 1 || it.defaultSign == 1 }?.coerceIn(0, list.lastIndex) ?: 0
            tvAmount1.text = list?.get(index)?.firstRepayment.formatAmount()
        }
        tvPeriod1.isSelected = selectPosition == position
        tvPeriodDays1.isSelected = selectPosition == position
        tvFirst.isSelected = selectPosition == position
        tvAmount1.isSelected = selectPosition == position
        tvNoInstall.isSelected = selectPosition == position
    }
}
