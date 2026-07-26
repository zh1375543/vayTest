package com.vaycore.finance.repayment.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.RepaymentAdapterBinding
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.copyToClipboard
import com.vaycore.finance.util.showToastMessage

class RepaymentAdapter :
    BaseAdapter<BankAccountResponse, RepaymentAdapterBinding>(RepaymentAdapterBinding::inflate) {

    override fun bindItem(
        binding: RepaymentAdapterBinding,
        item: BankAccountResponse,
        position: Int,
    ) = with(binding) {
        tvBankName.text = item.bankName ?:"-"
        tvBranchName.text = item.branchName ?: "-"
        tvAccount.text = item.bankAccount
        tvAccount.singleClick {
            tvAccount.text.toString().copyToClipboard()
            context.getString(R.string.copy_success).showToastMessage()
        }
    }
}