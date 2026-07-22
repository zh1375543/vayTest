package com.vaycore.finance.ui.sidepage.adapter

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
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasSavingRecord: Boolean,
    val hasPayoutRecord: Boolean,
)

/** Read-only date cells; transaction dates are represented by color only. */
class SavingsCalendarDayAdapter :
    RecyclerView.Adapter<SavingsCalendarDayAdapter.DayViewHolder>() {

    private val items = mutableListOf<CalendarDayItem>()

    fun submitItems(newItems: List<CalendarDayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemSavingsCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        binding.root.isClickable = false
        binding.root.isFocusable = false
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
                    if (item.isToday) {
                        ContextCompat.getColor(context, R.color.color_7087F8)
                    } else {
                        Color.TRANSPARENT
                    },
                )
            }
            setTextColor(
                when {
                    item.isToday -> ContextCompat.getColor(context, R.color.white)
                    !item.isCurrentMonth -> ContextCompat.getColor(context, R.color.C_D2D5DA)
                    item.hasPayoutRecord -> ContextCompat.getColor(context, R.color.C_FA560D)
                    item.hasSavingRecord -> ContextCompat.getColor(context, R.color.color_7087F8)
                    else -> ContextCompat.getColor(context, R.color.C_374151)
                },
            )
        }
    }

    override fun getItemCount(): Int = items.size

    class DayViewHolder(val binding: ItemSavingsCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root)
}
