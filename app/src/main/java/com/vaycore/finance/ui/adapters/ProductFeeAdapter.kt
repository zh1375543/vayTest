package com.vaycore.finance.ui.adapters

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductFeeBean
import com.vaycore.finance.databinding.ItemProductFeeBinding
import com.vaycore.finance.util.formatAmount

class ProductFeeAdapter :
    BaseAdapter<ProductFeeBean, ItemProductFeeBinding>(ItemProductFeeBinding::inflate) {

    override fun bindItem(
        binding: ItemProductFeeBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvFee.text = item.amount.formatAmount()
        tvTitle.text = item.getFeeName()
    }
}
