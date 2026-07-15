package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.MessageRecord
import com.vaycore.finance.databinding.ItemNoticeBinding

class NoticeAdapter :
    BaseAdapter<MessageRecord, ItemNoticeBinding>(ItemNoticeBinding::inflate) {

    override fun bindItem(
        binding: ItemNoticeBinding,
        item: MessageRecord,
        position: Int,
    ) = with(binding) {
        tvTitle.text = item.theme
        tvDesc.text = item.content
        tvDate.text = item.getTime()
        tvDot.isVisible = !item.readStatus
    }
}
