package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.vaycore.finance.data.local.bean.SelectionOption
import java.util.Calendar

/** Year-month wheel restricted to January 2020 through the current month. */
class WheelYearMonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val yearWheel = WheelView(context)
    private val monthWheel = WheelView(context)
    private val today = Calendar.getInstance()
    private val currentYear = today.get(Calendar.YEAR)
    private val currentMonth = today.get(Calendar.MONTH) + 1

    private var selectedYear = currentYear
    private var selectedMonth = currentMonth

    init {
        orientation = HORIZONTAL
        addView(yearWheel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(monthWheel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        yearWheel.setOnSelectListener { _, option ->
            selectedYear = option.info.toIntOrNull() ?: selectedYear
            refreshMonths()
        }
        monthWheel.setOnSelectListener { _, option ->
            selectedMonth = option.info.toIntOrNull() ?: selectedMonth
        }
        setSelectedYearMonth(currentYear, currentMonth)
    }

    fun setSelectedYearMonth(year: Int, month: Int) {
        selectedYear = year.coerceIn(MIN_YEAR, currentYear)
        selectedMonth = month.coerceIn(1, MONTH_COUNT)
        yearWheel.setData(
            (MIN_YEAR..currentYear).map { SelectionOption(it.toString()) },
            selectedYear - MIN_YEAR,
        )
        refreshMonths()
    }

    fun getSelectedYear(): Int = selectedYear

    fun getSelectedMonth(): Int = selectedMonth

    private fun refreshMonths() {
        monthWheel.setData(
            (1..MONTH_COUNT).map { SelectionOption(it.toString()) },
            selectedMonth - 1,
        )
    }

    companion object {
        private const val MIN_YEAR = 2020
        private const val MONTH_COUNT = 12
    }
}
