package com.vaycore.finance.ui.adapters

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.CustomerContactConfig
import com.vaycore.finance.data.local.language
import com.vaycore.finance.databinding.ItemContactWayBinding
import com.vaycore.finance.util.dialNumber
import com.vaycore.finance.util.copyToClipboard
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import kotlin.text.filter
import kotlin.text.isDigit

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
