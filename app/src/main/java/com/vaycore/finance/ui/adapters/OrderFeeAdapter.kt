package com.vaycore.finance.ui.adapters

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductFeeBean
import com.vaycore.finance.databinding.FeeAdapterBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class OrderFeeAdapter :
    BaseAdapter<ProductFeeBean, FeeAdapterBinding>(FeeAdapterBinding::inflate) {

    var currencySymbol: String? = null

    override fun bindItem(
        binding: FeeAdapterBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvFee.text = item.amount.formatAmountWithPrefix(currencySymbol)
        tvTitle.text = item.getFeeName()
    }
}
