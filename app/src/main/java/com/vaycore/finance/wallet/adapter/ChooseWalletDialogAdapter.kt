package com.vaycore.finance.wallet.adapter

import androidx.core.view.isVisible
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.model.wallet.WalletResponse
import com.vaycore.finance.databinding.ChooseWalletDialogAdapterBinding

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
