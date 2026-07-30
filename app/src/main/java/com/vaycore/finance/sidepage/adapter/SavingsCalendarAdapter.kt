package com.vaycore.finance.sidepage.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vaycore.finance.R
import com.vaycore.finance.databinding.ItemSavingsCalendarDayBinding

data class SavingsMonthItem(val year: Int, val month: Int)

data class CalendarDayItem(
    val day: Int,
    val date: String? = null,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val hasSavingRecord: Boolean,
    val hasPayoutRecord: Boolean,
)

class SavingsCalendarDayAdapter :
    RecyclerView.Adapter<SavingsCalendarDayAdapter.DayViewHolder>() {

    private val items = mutableListOf<CalendarDayItem>()
    private var onDayClick: ((CalendarDayItem) -> Unit)? = null

    fun setOnDayClickListener(listener: (CalendarDayItem) -> Unit) {
        onDayClick = listener
    }

    fun submitItems(newItems: List<CalendarDayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun selectDate(date: String) {
        for (index in items.indices) {
            val item = items[index]
            val isSelected = item.date == date
            if (item.isSelected == isSelected) continue

            items[index] = item.copy(isSelected = isSelected)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemSavingsCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.binding.tvDay.apply {
            text = item.day.toString()
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    if (item.isSelected) {
                        ContextCompat.getColor(context, R.color.brand_primary)
                    } else {
                        Color.TRANSPARENT
                    },
                )
            }
            setTextColor(
                when {
                    item.isSelected -> ContextCompat.getColor(context, R.color.text_inverse)
                    !item.isCurrentMonth -> ContextCompat.getColor(context, R.color.border_default)
                    item.hasPayoutRecord -> ContextCompat.getColor(context, R.color.action_withdraw)
                    item.hasSavingRecord -> ContextCompat.getColor(context, R.color.brand_primary)
                    else -> ContextCompat.getColor(context, R.color.text_body)
                },
            )
        }
        holder.binding.root.apply {
            isEnabled = item.isCurrentMonth && !item.date.isNullOrBlank()
            isClickable = isEnabled
            isFocusable = isEnabled
            setOnClickListener {
                onDayClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class DayViewHolder(val binding: ItemSavingsCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
