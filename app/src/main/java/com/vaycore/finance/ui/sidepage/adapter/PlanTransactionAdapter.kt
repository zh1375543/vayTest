package com.vaycore.finance.ui.sidepage.adapter

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemSidepagePlanTransactionBinding

data class PlanTransactionItem(
    val id: Int? = null,
    val title: String,
    val amount: String,
    val date: String,
    val address: String? = null,
    val remark: String? = null,
    val imageUrls: String? = null,
    val isSaving: Boolean,
)

/** Renders the temporary transaction records shown on the plan details screen. */
class PlanTransactionAdapter :
    BaseAdapter<PlanTransactionItem, ItemSidepagePlanTransactionBinding>(
        ItemSidepagePlanTransactionBinding::inflate,
    ) {

    override fun bindItem(
        binding: ItemSidepagePlanTransactionBinding,
        item: PlanTransactionItem,
        position: Int,
    ) = with(binding) {
        tvTransactionTitle.text = item.title
        tvTransactionAmount.text = item.amount
        tvTransactionDate.text = item.date
        val hasAddress = !item.address.isNullOrBlank()
        ivAddress.isVisible = hasAddress
        tvTransactionAddress.isVisible = hasAddress
        tvTransactionAddress.text = item.address.orEmpty()
        ivTransactionType.setImageResource(
            if (item.isSaving) R.mipmap.ic_save_img else R.mipmap.ic_at_payout,
        )
    }
}
