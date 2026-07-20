package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.AuthOptionResponse
import com.vaycore.finance.databinding.ItemAuthEntryBinding

class AuthEntryAdapter : BaseAdapter<AuthOptionResponse, ItemAuthEntryBinding>(ItemAuthEntryBinding::inflate) {

    override fun bindItem(
        binding: ItemAuthEntryBinding,
        item: AuthOptionResponse,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.title
        tvTitle.isSelected = item.isCertified
        ivIcon.setImageResource(item.src)
        ivRight.setImageResource(
            if (item.isCertified) R.mipmap.ic_auth_statu else R.mipmap.mine_right
        )
    }
}
