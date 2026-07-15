// DateWheelView.kt
package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.vaycore.finance.data.local.bean.SelectionOption
import java.util.Calendar
import java.util.Locale

class WheelDateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val yearWheel = WheelView(context)
    private val monthWheel = WheelView(context)
    private val dayWheel = WheelView(context)

    private val minYear = 1900
    private val today = Calendar.getInstance()

    // single source of truth, maintained here
    private var selectedYear = today.get(Calendar.YEAR) - 18
    private var selectedMonth = today.get(Calendar.MONTH) + 1
    private var selectedDay = today.get(Calendar.DAY_OF_MONTH)

    init {
        orientation = HORIZONTAL
        addView(yearWheel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(monthWheel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(dayWheel, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        setup()
    }

    private fun setup() {
        yearWheel.setOnSelectListener { _, v ->
            selectedYear = v.info.toIntOrNull() ?: selectedYear
            refreshMonthWheel()
        }
        monthWheel.setOnSelectListener { _, v ->
            selectedMonth = v.info.toIntOrNull() ?: selectedMonth
            refreshDayWheel()
        }
        dayWheel.setOnSelectListener { _, v ->
            selectedDay = v.info.toIntOrNull() ?: selectedDay
        }

        val years = (minYear..today.get(Calendar.YEAR)).map { SelectionOption(it.toString()) }
        yearWheel.setData(years, selectedYear - minYear)

        val months = makeMonths(selectedYear)
        monthWheel.setData(months, selectedMonth - 1)

        val days = makeDays(selectedYear, selectedMonth)
        dayWheel.setData(days, selectedDay - 1)
    }

    // when year changes, refresh both month and day
    private fun refreshMonthWheel() {
        val maxMonth = if (selectedYear == today.get(Calendar.YEAR)) {
            today.get(Calendar.MONTH) + 1
        } else 12
        selectedMonth = selectedMonth.coerceAtMost(maxMonth)

        val months = makeMonths(selectedYear)
        monthWheel.setData(months, selectedMonth - 1)

        refreshDayWheel()
    }

    private fun refreshDayWheel() {
        val maxDay = getMaxDay(selectedYear, selectedMonth)
        selectedDay = selectedDay.coerceAtMost(maxDay)
        dayWheel.setData(makeDays(selectedYear, selectedMonth), selectedDay - 1)
    }

    private fun makeMonths(year: Int): List<SelectionOption> {
        val maxMonth = if (year == today.get(Calendar.YEAR)) {
            today.get(Calendar.MONTH) + 1
        } else 12
        return (1..maxMonth).map { SelectionOption(it.toString()) }
    }

    private fun makeDays(year: Int, month: Int): List<SelectionOption> {
        return (1..getMaxDay(year, month)).map { SelectionOption(it.toString()) }
    }

    private fun getMaxDay(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) + 1) {
            max.coerceAtMost(today.get(Calendar.DAY_OF_MONTH))
        } else max
    }

    fun getSelectedCalendar(): Calendar = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth - 1, selectedDay)
    }

    fun getDateString(): String = String.format(
        Locale.getDefault(),
        "%d-%02d-%02d",
        selectedDay,
        selectedMonth,
        selectedYear
    )
}