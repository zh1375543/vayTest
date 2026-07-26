package com.vaycore.finance.wallet.adapter

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemBankCardBinding
import com.vaycore.finance.model.wallet.BankAccountResponse
import com.vaycore.finance.ui.binding.BindableItemsAdapter

class BankCardListAdapter :
    BaseAdapter<BankAccountResponse, ItemBankCardBinding>(ItemBankCardBinding::inflate),
    BindableItemsAdapter {

    override fun submitBindingItems(items: List<*>?) {
        submitItems(items.orEmpty().filterIsInstance<BankAccountResponse>())
    }

    override fun bindItem(
        binding: ItemBankCardBinding,
        item: BankAccountResponse,
        position: Int,
    ) {
        binding.item = item
        binding.executePendingBindings()
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
