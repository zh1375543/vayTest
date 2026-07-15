package com.vaycore.finance.ui.adapters

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemHomeTagBinding

class HomeTagAdapter : BaseAdapter<String, ItemHomeTagBinding>(ItemHomeTagBinding::inflate) {

    override fun bindItem(
        binding: ItemHomeTagBinding,
        item: String,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item
        tvTitle.isSelected = position % 2 == 0
    }
}
