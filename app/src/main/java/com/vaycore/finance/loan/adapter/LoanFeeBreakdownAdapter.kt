package com.vaycore.finance.loan.adapter

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.model.loan.ProductFeeBean
import com.vaycore.finance.databinding.ItemProductFeeBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class LoanFeeBreakdownAdapter :
    BaseAdapter<ProductFeeBean, ItemProductFeeBinding>(ItemProductFeeBinding::inflate) {

    override fun bindItem(
        binding: ItemProductFeeBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvFee.text = item.amount.formatAmountWithPrefix()
        tvTitle.text = item.getFeeName()
    }
}
