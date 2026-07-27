package com.vaycore.finance.home

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemHomeProductBinding
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.platform.formatLoanTerm

class LoanCatalogAdapter :
    BaseAdapter<ProductBean, ItemHomeProductBinding>(ItemHomeProductBinding::inflate) {

    override fun bindItem(
        binding: ItemHomeProductBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        ivIcon.loadImage(item.productImageUrl, R.mipmap.ic_product_defalut_img)
        tvName.text = item.productName
        tvLoanAmount.text = context.getString(R.string.home_product_loan_amount_title)
        tvAmount.text =
            if (item.canApply) item.maxLoanAmount.formatAmountWithPrefix(item.currencySymbol) else item.loanAmountRange
        tvDays.text = context.formatLoanTerm(item.timeLimit)
        tvApply.isEnabled = item.canApply
        enableView.isVisible = !tvApply.isEnabled
        ivNew.isVisible = item.newSign == 1 && !item.isTogether
        rvTag.adapter = LoanFeatureTagAdapter().apply {
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
