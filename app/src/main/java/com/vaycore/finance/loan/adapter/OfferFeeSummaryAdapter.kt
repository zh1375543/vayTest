package com.vaycore.finance.loan.adapter

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ProductHeaderFeeAdapterBinding
import com.vaycore.finance.model.loan.ProductFeeBean
import com.vaycore.finance.util.formatAmountWithPrefix

class OfferFeeSummaryAdapter :
    BaseAdapter<ProductFeeBean, ProductHeaderFeeAdapterBinding>(ProductHeaderFeeAdapterBinding::inflate) {

    override fun bindItem(
        binding: ProductHeaderFeeAdapterBinding,
        item: ProductFeeBean,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.getFeeName()
        tvFee.text = item.amount.formatAmountWithPrefix()
    }
}
