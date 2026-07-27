package com.vaycore.finance.message

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.databinding.ItemNoticeBinding
import com.vaycore.finance.model.message.MessageRecord
import com.vaycore.finance.ui.binding.BindableItemsAdapter

class MessageInboxAdapter :
    BaseAdapter<MessageRecord, ItemNoticeBinding>(ItemNoticeBinding::inflate), BindableItemsAdapter {

    override fun submitBindingItems(items: List<*>?) {
        submitItems(items.orEmpty().filterIsInstance<MessageRecord>())
    }

    override fun bindItem(
        binding: ItemNoticeBinding,
        item: MessageRecord,
        position: Int,
    ) {
        binding.item = item
        binding.executePendingBindings()
    }
}
