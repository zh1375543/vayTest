package com.vaycore.finance.ui

import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.bean.SelectionOption
import com.vaycore.finance.databinding.ItemAddressOptionBinding
import java.text.Normalizer
import java.util.Locale

internal class AddressOptionAdapter :
    BaseAdapter<SelectionOption, ItemAddressOptionBinding>(ItemAddressOptionBinding::inflate) {

    private var selectedPosition = -1

    override fun bindItem(
        binding: ItemAddressOptionBinding,
        item: SelectionOption,
        position: Int,
    ) = with(binding) {
        val sectionLetter = addressInitial(item.info)
        val previousLetter = items.getOrNull(position - 1)?.let { addressInitial(it.info) }
        tvSectionLetter.text = sectionLetter.toString()
        tvSectionLetter.isInvisible = position > 0 && sectionLetter == previousLetter
        tvName.text = item.info
        tvName.setTextColor(
            ContextCompat.getColor(
                context,
                if (position == selectedPosition) R.color.brand_primary else R.color.text_body,
            ),
        )
    }

    fun select(position: Int) {
        if (position !in items.indices || position == selectedPosition) return
        val previousPosition = selectedPosition
        selectedPosition = position
        if (previousPosition in items.indices) notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    fun resetSelection() {
        selectedPosition = -1
    }
}

internal fun sortAddressOptions(items: List<SelectionOption>): List<SelectionOption> =
    items.sortedWith(
        compareBy<SelectionOption>(
            { if (addressInitial(it.info) == '#') 1 else 0 },
            { addressInitial(it.info) },
            { normalizedAddressName(it.info) },
        ),
    )

internal fun addressInitial(name: String): Char {
    val initial = normalizedAddressName(name).firstOrNull { it.isLetter() } ?: return '#'
    return initial.takeIf { it in 'A'..'Z' } ?: '#'
}

private fun normalizedAddressName(name: String): String =
    Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
        .replace("Đ", "D")
        .replace("đ", "D")
        .replace(Regex("\\p{M}+"), "")
        .uppercase(Locale.ENGLISH)
