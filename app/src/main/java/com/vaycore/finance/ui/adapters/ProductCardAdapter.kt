package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.ItemProductCardBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class ProductCardAdapter :
    BaseAdapter<ProductBean, ItemProductCardBinding>(ItemProductCardBinding::inflate) {

    override fun bindItem(
        binding: ItemProductCardBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        tvAmount.text = item.maxLoanAmount.formatAmountWithPrefix(item.currencySymbol)
        tvName.text = item.productName
        tvLoan.text = context.getString(R.string.loan_amount).replace("(%s)", "")
    }
}
