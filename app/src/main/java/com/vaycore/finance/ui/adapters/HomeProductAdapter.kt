package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.ItemHomeProductBinding
import com.vaycore.finance.util.formatDays
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.formatAmount

class HomeProductAdapter :
    BaseAdapter<ProductBean, ItemHomeProductBinding>(ItemHomeProductBinding::inflate) {

    override fun bindItem(
        binding: ItemHomeProductBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        ivIcon.loadImage(item.productImageUrl, R.mipmap.ic_order_defalut_img)
        tvName.text = item.productName
        tvLoanAmount.text = context.getString(R.string.home_product_loan_amount_title)
        tvAmount.text =
            if (item.canApply && item.isFillBank) item.maxLoanAmount.formatAmount(item.currencySymbol) else item.loanAmountRange
        tvDays.text = context.formatDays(item.timeLimit)
        tvApply.isEnabled = item.canApply
        enableView.isVisible = !tvApply.isEnabled
        ivNew.isVisible = item.newSign == 1 && !item.isTogether
        rvTag.adapter = HomeTagAdapter().apply {
            submitItems(item.tagList?.distinct())
        }
        tvApply.text =
            context.getString(if (item.showConditionTypeSign != "1") R.string.withdrawal else R.string.go_add_info_str)
    }

    override fun bindChildClickListeners(
        binding: ItemHomeProductBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        super.bindChildClickListeners(binding, item, position)
        tvApply.setOnClickListener {
            dispatchChildClick(it, item, position)
        }
    }
}
