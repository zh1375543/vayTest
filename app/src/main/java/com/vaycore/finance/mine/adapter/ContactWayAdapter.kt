package com.vaycore.finance.mine.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.language
import com.vaycore.finance.databinding.ItemContactWayBinding
import com.vaycore.finance.model.home.CustomerContactConfig
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.copyToClipboard
import com.vaycore.finance.util.dialNumber
import com.vaycore.finance.util.showToastMessage

class ContactWayAdapter :
    BaseAdapter<CustomerContactConfig, ItemContactWayBinding>(ItemContactWayBinding::inflate) {

    override fun bindItem(
        binding: ItemContactWayBinding,
        item: CustomerContactConfig,
        position: Int,
    ) = with(binding) {
        val title = if (language == "vi") item.vernacularTitle else item.enTitle
        tvTelegramTitle.text = title
        tvTelegram.text = item.content

        ivTelegram.setImageResource(
            when (title) {
                context.getString(R.string.phone_number) -> R.mipmap.ic_contact_call
                context.getString(R.string.email) -> R.mipmap.ic_contact_ems
                else -> R.mipmap.ic_contact_tg
            }
        )

        tvCopyTelegram.text =
            context.getString(if (item.buttonType == 2) R.string.call else R.string.copy)

        tvCopyTelegram.singleClick { _ ->
            if (item.buttonType == 2) {
                item.content?.filter { it1 -> it1.isDigit() }?.dialNumber()
            } else {
                item.content?.copyToClipboard()
                context.getString(R.string.copy_success).showToastMessage()
            }
        }
    }
}