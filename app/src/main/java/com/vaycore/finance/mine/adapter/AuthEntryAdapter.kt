package com.vaycore.finance.mine.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemAuthEntryBinding
import com.vaycore.finance.model.identity.AuthOptionResponse

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