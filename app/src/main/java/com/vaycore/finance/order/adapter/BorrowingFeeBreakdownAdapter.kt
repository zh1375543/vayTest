package com.vaycore.finance.order.adapter

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.model.loan.ProductFeeBean
import com.vaycore.finance.databinding.ItemBorrowingFeeBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class BorrowingFeeBreakdownAdapter :
    BaseAdapter<ProductFeeBean, ItemBorrowingFeeBinding>(ItemBorrowingFeeBinding::inflate) {

    var currencySymbol: String? = null

    override fun bindItem(
        binding: ItemBorrowingFeeBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvFee.text = item.amount.formatAmountWithPrefix(currencySymbol)
        tvTitle.text = item.getFeeName()
    }
}
