package com.vaycore.finance.ui.adapters

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ProductHeaderFeeAdapterBinding
import com.vaycore.finance.data.local.bean.ProductFeeBean
import com.vaycore.finance.util.formatAmount

class ProductHeaderFeeAdapter :
    BaseAdapter<ProductFeeBean, ProductHeaderFeeAdapterBinding>(ProductHeaderFeeAdapterBinding::inflate) {

    override fun bindItem(
        binding: ProductHeaderFeeAdapterBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.getFeeName()
        tvFee.text = item.amount.formatAmount()
    }
}
