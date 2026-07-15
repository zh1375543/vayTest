package com.vaycore.finance.ui.adapters

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
        tvCard.text = item.account ?: item.bankNo
        tvBank.text = item.name ?: item.bankName
        ivCard.isSelected = selectPosition == position
    }
}