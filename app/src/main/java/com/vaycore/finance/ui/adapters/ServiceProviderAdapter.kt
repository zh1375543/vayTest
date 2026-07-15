package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.AuthOptionResponse
import com.vaycore.finance.databinding.ItemServiceProviderBinding

class ServiceProviderAdapter :
    BaseAdapter<AuthOptionResponse, ItemServiceProviderBinding>(ItemServiceProviderBinding::inflate) {

    override fun bindItem(
        binding: ItemServiceProviderBinding,
        item: AuthOptionResponse,
        position: Int,
    ) = with(binding) {
        ivIcon.setImageResource(
            when (position) {
                0 -> if (item.isCertified) R.mipmap.carrier_viettel_on else R.mipmap.carrier_viettel
                1 -> if (item.isCertified) R.mipmap.carrier_mobifone_on else R.mipmap.carrier_mobifone
                2 -> if (item.isCertified) R.mipmap.carrier_vinaphone_on else R.mipmap.carrier_vinaphone
                3 -> if (item.isCertified) R.mipmap.carrier_vietnamobile_on else R.mipmap.carrier_vietnamobile
                else -> if (item.isCertified) R.mipmap.carrier_saymee_on else R.mipmap.carrier_saymee
            }
        )
        rootView.isSelected = item.isCertified
        ivCheck.isSelected = item.isCertified
        tvName.text = item.type
        tvName.isSelected = item.isCertified
    }
}
