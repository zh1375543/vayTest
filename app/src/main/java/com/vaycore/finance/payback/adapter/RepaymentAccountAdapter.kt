package com.vaycore.finance.payback.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemRepaymentAccountBinding
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.ExternalActionLauncher
import com.vaycore.finance.util.showToastMessage

class RepaymentAccountAdapter :
    BaseAdapter<BankAccountResponse, ItemRepaymentAccountBinding>(ItemRepaymentAccountBinding::inflate) {

    override fun bindItem(
        binding: ItemRepaymentAccountBinding,
        item: BankAccountResponse,
        position: Int,
    ) = with(binding) {
        tvBankName.text = item.bankName ?:"-"
        tvBranchName.text = item.branchName ?: "-"
        tvAccount.text = item.bankAccount
        tvAccount.singleClick {
            ExternalActionLauncher.copyText(context, tvAccount.text.toString())
            context.getString(R.string.copy_success).showToastMessage()
        }
    }
}
