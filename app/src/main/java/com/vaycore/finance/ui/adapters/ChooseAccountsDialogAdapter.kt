package com.vaycore.finance.ui.adapters

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.databinding.AddAccountDialogAdapterBinding

class ChooseAccountsDialogAdapter(var selectPosition: Int) :
    BaseAdapter<BankAccountResponse, AddAccountDialogAdapterBinding>(AddAccountDialogAdapterBinding::inflate) {

    override fun bindItem(
        binding: AddAccountDialogAdapterBinding,
        item: BankAccountResponse,
        position: Int,
    ) = with(binding) {
        val isWallet = item.payWay == "WALLET"
        val isSelected = selectPosition == position

        tvCard.text = item.account ?: item.bankNo
        tvBank.text = item.name ?: item.bankName
        ivAccountIcon.setImageResource(
            if (isWallet) R.mipmap.ic_wallet_defalut else R.mipmap.ic_bank_default
        )
        ivBankState.isVisible = !isWallet
        accountItemBackground.isSelected = isSelected

        val textColor = ContextCompat.getColor(
            context,
            if (isSelected) R.color.white else R.color.C_374151,
        )
        tvCard.setTextColor(textColor)
        tvBank.setTextColor(textColor)
    }
}
