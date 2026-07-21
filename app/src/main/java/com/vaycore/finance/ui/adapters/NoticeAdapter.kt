package com.vaycore.finance.ui.adapters

import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.MessageRecord
import com.vaycore.finance.databinding.ItemNoticeBinding
import com.vaycore.finance.ui.binding.BindableItemsAdapter

class NoticeAdapter :
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
