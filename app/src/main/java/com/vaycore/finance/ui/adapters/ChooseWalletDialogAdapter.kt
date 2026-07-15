package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.WalletResponse
import com.vaycore.finance.databinding.ChooseWalletDialogAdapterBinding
import com.vaycore.finance.ui.extension.loadImage

class ChooseWalletDialogAdapter :
    BaseAdapter<WalletResponse, ChooseWalletDialogAdapterBinding>(ChooseWalletDialogAdapterBinding::inflate) {

    override fun bindItem(
        binding: ChooseWalletDialogAdapterBinding,
        item: WalletResponse,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.walletName.orEmpty()
        divider.isVisible = position < items.lastIndex
    }
}
