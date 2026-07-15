package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.BankChannelResponse
import com.vaycore.finance.databinding.ChooseBankDialogAdapterBinding
import com.vaycore.finance.ui.extension.loadImage
import androidx.core.view.isVisible

class ChooseBankDialogAdapter :
    BaseAdapter<BankChannelResponse, ChooseBankDialogAdapterBinding>(ChooseBankDialogAdapterBinding::inflate) {

    override fun bindItem(
        binding: ChooseBankDialogAdapterBinding,
        item: BankChannelResponse,
        position: Int,
    ) = with(binding) {
        ivBank.loadImage(item.logoUrl, R.mipmap.ic_bank_default)
        tvTitle.text = item.longCode
        tvContent.text = item.bankName
        divider.isVisible = position < items.lastIndex
    }
}
