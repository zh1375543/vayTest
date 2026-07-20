package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.databinding.ItemBankCardBinding

class BankCardListAdapter :
    BaseAdapter<BankAccountResponse, ItemBankCardBinding>(ItemBankCardBinding::inflate) {

    override fun bindItem(
        binding: ItemBankCardBinding,
        item: BankAccountResponse,
        position: Int,
    ) = with(binding) {
        val isWallet = item.payWay == "WALLET"
        val isDefault = item.isDefault == 1
        menuGroup.isVisible = !isDefault
        compactBottomSpace.isVisible = isDefault
        ivAccountIcon.setImageResource(
            if (isWallet) R.mipmap.ic_wallet_defalut else R.mipmap.ic_bank_default
        )
        tvBankName.text = item.bankName
        tvBankCard.text = item.bankNo
    }

    override fun bindChildClickListeners(
        binding: ItemBankCardBinding,
        item: BankAccountResponse,
        position: Int,
    ) = with(binding) {
        super.bindChildClickListeners(binding, item, position)
        listOf(tvDelete, tvDefault).forEach { view ->
            view.setOnClickListener {
                dispatchChildClick(it, item, position)
            }
        }
    }
}
