package com.vaycore.finance.home

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemHomeProductBinding
import com.vaycore.finance.home.state.HomeProductUi
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.platform.formatLoanTerm

class LoanCatalogAdapter :
    BaseAdapter<HomeProductUi, ItemHomeProductBinding>(ItemHomeProductBinding::inflate) {

    override fun bindItem(
        binding: ItemHomeProductBinding,
        item: HomeProductUi,
        position: Int,
    ) = with(binding) {
        val product = item.product
        ivIcon.loadImage(product.productImageUrl, R.mipmap.ic_product_defalut_img)
        tvName.text = product.productName
        tvLoanAmount.text = context.getString(R.string.home_product_loan_amount_title)
        tvAmount.text =
            if (item.canApply) product.maxLoanAmount.formatAmountWithPrefix(product.currencySymbol) else product.loanAmountRange
        tvDays.text = context.formatLoanTerm(product.timeLimit)
        tvApply.isEnabled = item.canApply
        enableView.isVisible = !tvApply.isEnabled
        ivNew.isVisible = product.newSign == 1 && !product.isTogether
        rvTag.adapter = LoanFeatureTagAdapter().apply {
            submitItems(product.tagList?.distinct())
        }
        tvApply.text =
            context.getString(if (product.showConditionTypeSign != "1") R.string.withdrawal else R.string.go_add_info_str)
    }

    override fun bindChildClickListeners(
        binding: ItemHomeProductBinding,
        item: HomeProductUi,
        position: Int,
    ) = with(binding) {
        super.bindChildClickListeners(binding, item, position)
        tvApply.setOnClickListener {
            dispatchChildClick(it, item, position)
        }
    }
}
