package com.vaycore.finance.ui.sidepage.act

import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.sideBean.CalendarDay
import com.vaycore.finance.data.local.sideBean.PlanCalendarResponse
import com.vaycore.finance.databinding.SidepageSavingsCalendarActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showYearMonthPickerDialog
import com.vaycore.finance.ui.sidepage.adapter.CalendarDayItem
import com.vaycore.finance.ui.sidepage.adapter.PlanTransactionAdapter
import com.vaycore.finance.ui.sidepage.adapter.PlanTransactionItem
import com.vaycore.finance.ui.sidepage.adapter.SavingsCalendarDayAdapter
import com.vaycore.finance.ui.sidepage.adapter.SavingsMonthItem
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding
import java.util.Calendar
import java.util.Locale

/** Displays monthly savings records with a constrained year-month picker. */
class SavingsCalendarActivity : BaseActivity<SidepageSavingsCalendarActivityBinding>() {

    override val binding by viewBinding(SidepageSavingsCalendarActivityBinding::inflate)

    private val viewModel by viewModels<SideHomeViewModel>()
    private val dayAdapter by lazy { SavingsCalendarDayAdapter() }
    private val transactionAdapter by lazy { PlanTransactionAdapter() }
    private val today = Calendar.getInstance()
    private var selectedYear = today.get(Calendar.YEAR)
    private var selectedMonth = today.get(Calendar.MONTH) + 1

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)

        monthSelector.singleClick {
            showYearMonthPickerDialog(selectedYear, selectedMonth) { year, month ->
                selectedYear = year
                selectedMonth = month
                loadPlanCalendar()
            }
        }
        rvCalendar.apply {
            layoutManager = GridLayoutManager(this@SavingsCalendarActivity, DAYS_IN_WEEK)
            adapter = dayAdapter
            isNestedScrollingEnabled = false
        }
        rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@SavingsCalendarActivity)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }

        loadingLayout.setOnRetryClickListener { loadPlanCalendar() }
        loadPlanCalendar()
    }

    override fun initObserve() = with(viewModel) {
        planCalendarResult.observe(this@SavingsCalendarActivity) { result ->
            renderCalendar(result)
        }
        planCalendarFailed.observe(this@SavingsCalendarActivity) { event ->
            if (event.getContentIfNotHandled() != null) {
                binding.loadingLayout.showError()
            }
        }
    }

    private fun loadPlanCalendar() {
        binding.loadingLayout.showLoading()
        viewModel.getPlanCalendar(selectedYear, selectedMonth)
    }

    private fun renderCalendar(response: PlanCalendarResponse?) = with(binding) {
        val month = SavingsMonthItem(selectedYear, selectedMonth)
        tvMonth.text = getString(R.string.portal_year_month_value, month.year, month.month)
        val days = response?.days.orEmpty()
        val transactions = days.flatMap { day -> day.toTransactionItems() }
        dayAdapter.submitItems(buildCalendarDays(month, days))
        transactionAdapter.submitItems(transactions)
        rvTransactions.isVisible = transactions.isNotEmpty()
        tvNoTransactions.isVisible = false
        loadingLayout.showContent()
    }

    private fun buildCalendarDays(
        month: SavingsMonthItem,
        calendarDays: List<CalendarDay>,
    ): List<CalendarDayItem> {
        val firstDay = Calendar.getInstance().apply {
            clear()
            set(month.year, month.month - 1, 1)
        }
        val leadingCount = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val previousMonth = (firstDay.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val daysInPreviousMonth = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        val currentMonthDays = calendarDays.mapNotNull { calendarDay ->
            val day = calendarDay.day ?: calendarDay.date?.takeLast(2)?.toIntOrNull()
            day?.let { it to calendarDay }
        }.toMap()

        return List(CALENDAR_CELL_COUNT) { index ->
            val day: Int
            val isCurrentMonth: Boolean
            when {
                index < leadingCount -> {
                    day = daysInPreviousMonth - leadingCount + index + 1
                    isCurrentMonth = false
                }
                index < leadingCount + daysInMonth -> {
                    day = index - leadingCount + 1
                    isCurrentMonth = true
                }
                else -> {
                    day = index - leadingCount - daysInMonth + 1
                    isCurrentMonth = false
                }
            }

            val calendarDay = if (isCurrentMonth) currentMonthDays[day] else null
            CalendarDayItem(
                day = day,
                isCurrentMonth = isCurrentMonth,
                isToday = isCurrentMonth &&
                    today.get(Calendar.YEAR) == month.year &&
                    today.get(Calendar.MONTH) + 1 == month.month &&
                    today.get(Calendar.DAY_OF_MONTH) == day,
                hasSavingRecord = calendarDay?.saveAmount?.signum() == 1,
                hasPayoutRecord = calendarDay?.withdrawAmount?.signum() == 1,
            )
        }
    }

    private fun CalendarDay.toTransactionItems(): List<PlanTransactionItem> {
        val date = date.orEmpty()
        return buildList {
            saveAmount?.takeIf { it.signum() == 1 }?.let { amount ->
                add(
                    PlanTransactionItem(
                        title = getString(R.string.portal_save_money),
                        amount = amount.formatAmountWithPrefix(),
                        date = date,
                        isSaving = true,
                    ),
                )
            }
            withdrawAmount?.takeIf { it.signum() == 1 }?.let { amount ->
                add(
                    PlanTransactionItem(
                        title = getString(R.string.portal_withdraw),
                        amount = amount.formatAmountWithPrefix(),
                        date = date,
                        isSaving = false,
                    ),
                )
            }
        }
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
        private const val CALENDAR_CELL_COUNT = 42

    }
}
