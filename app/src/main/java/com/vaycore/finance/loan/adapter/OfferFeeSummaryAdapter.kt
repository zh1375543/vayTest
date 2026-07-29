package com.vaycore.finance.loan.adapter

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemOfferFeeSummaryBinding
import com.vaycore.finance.model.loan.ProductFeeBean
import com.vaycore.finance.util.formatAmountWithPrefix

class OfferFeeSummaryAdapter :
    BaseAdapter<ProductFeeBean, ItemOfferFeeSummaryBinding>(ItemOfferFeeSummaryBinding::inflate) {

    override fun bindItem(
        binding: ItemOfferFeeSummaryBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.getFeeName()
        tvFee.text = item.amount.formatAmountWithPrefix()
    }
}
