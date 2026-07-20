package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.LoanResultAdapterBinding
import com.vaycore.finance.util.formatAmountWithPrefix

class LoanResultAdapter :
    BaseAdapter<ProductBean, LoanResultAdapterBinding>(LoanResultAdapterBinding::inflate) {

    override fun bindItem(
        binding: LoanResultAdapterBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        tvName.text = item.productName
        val a = String.format(context.getString(R.string.loan_amount), item.currency ?: "")
        tvLoanAmount.text = if (item.currency == null) a.replace("()", "") else a
        tvAmount.text = item.loanAmount.formatAmountWithPrefix(item.currencySymbol)
        ivStatus.isSelected = item.pushStatus == 200
        tvName.isSelected = item.pushStatus == 200
    }
}
