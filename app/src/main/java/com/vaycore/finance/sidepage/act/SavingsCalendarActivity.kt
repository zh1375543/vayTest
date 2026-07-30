package com.vaycore.finance.sidepage.act

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.model.side.CalendarDay
import com.vaycore.finance.model.side.PlanCalendarResponse
import com.vaycore.finance.model.side.RecordItem
import com.vaycore.finance.databinding.SidepageSavingsCalendarActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showYearMonthPickerDialog
import com.vaycore.finance.sidepage.adapter.CalendarDayItem
import com.vaycore.finance.sidepage.adapter.PlanTransactionAdapter
import com.vaycore.finance.sidepage.adapter.PlanTransactionItem
import com.vaycore.finance.sidepage.adapter.SavingsCalendarDayAdapter
import com.vaycore.finance.sidepage.adapter.SavingsMonthItem
import com.vaycore.finance.sidepage.SideHomeViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding
import java.util.Calendar
import java.util.Locale

/** Displays monthly savings records with a constrained year-month picker. */
class SavingsCalendarActivity : BaseActivity<SidepageSavingsCalendarActivityBinding>() {

    override val binding by viewBinding(SidepageSavingsCalendarActivityBinding::inflate)

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planId by lazy { intent.getIntExtra(EXTRA_PLAN_ID, INVALID_PLAN_ID) }
    private val dayAdapter by lazy { SavingsCalendarDayAdapter() }
    private val transactionAdapter by lazy { PlanTransactionAdapter() }
    private val today = Calendar.getInstance()
    private var selectedYear = today.get(Calendar.YEAR)
    private var selectedMonth = today.get(Calendar.MONTH) + 1
    private var selectedDate = today.toApiDate()

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
            itemAnimator = null
            isNestedScrollingEnabled = false
        }
        dayAdapter.setOnDayClickListener { item ->
            item.date?.takeIf { it != selectedDate }?.let { date ->
                dayAdapter.selectDate(date)
                loadRecordsForDate(date)
            }
        }
        rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@SavingsCalendarActivity)
            adapter = transactionAdapter
            itemAnimator = null
            isNestedScrollingEnabled = false
        }

        loadingLayout.setOnRetryClickListener { loadInitialData() }
        loadInitialData()
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
        recordListResult.observe(this@SavingsCalendarActivity) { result ->
            renderTransactions(result?.list.orEmpty())
        }
        recordListFailed.observe(this@SavingsCalendarActivity) { event ->
            if (event.getContentIfNotHandled() != null) {
                renderTransactions(emptyList())
            }
        }
    }

    private fun loadInitialData() {
        if (planId == INVALID_PLAN_ID) {
            binding.loadingLayout.showError()
            return
        }
        loadPlanCalendar()
        loadRecordsForDate(selectedDate)
    }

    private fun loadPlanCalendar() {
        binding.loadingLayout.showLoading()
        viewModel.getPlanCalendar(selectedYear, selectedMonth)
    }

    private fun renderCalendar(response: PlanCalendarResponse?) = with(binding) {
        val month = SavingsMonthItem(selectedYear, selectedMonth)
        tvMonth.text = getString(R.string.portal_year_month_value, month.year, month.month)
        val days = response?.days.orEmpty()
        dayAdapter.submitItems(buildCalendarDays(month, days))
        loadingLayout.showContent()
    }

    private fun loadRecordsForDate(date: String) {
        val apiDate = date.toApiDate() ?: return
        selectedDate = apiDate
        viewModel.getRecordList(
            planId = planId,
            startTime = "$apiDate 00:00:00",
            endTime = "$apiDate 23:59:59",
        )
    }

    private fun renderTransactions(records: List<RecordItem>) = with(binding) {
        transactionAdapter.submitItems(
            records.map { record -> record.toTransactionItem() },
            TRANSACTION_DIFF_CALLBACK,
        )
        rvTransactions.isVisible = records.isNotEmpty()
        tvNoTransactions.isVisible = records.isEmpty()
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
        val currentMonthDays = calendarDays.mapNotNull { calendarDay ->
            val day = calendarDay.day ?: calendarDay.date.toApiDate()?.takeLast(2)?.toIntOrNull()
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
                date = calendarDay?.date.toApiDate(),
                isCurrentMonth = isCurrentMonth,
                isSelected = calendarDay?.date.toApiDate() == selectedDate,
                hasSavingRecord = calendarDay?.saveAmount?.signum() == 1,
                hasPayoutRecord = calendarDay?.withdrawAmount?.signum() == 1,
            )
        }
    }

    private fun RecordItem.toTransactionItem() = PlanTransactionItem(
        id = id,
        title = recordTypeText.orEmpty(),
        amount = amount.formatAmountWithPrefix(),
        date = occurTime.orEmpty(),
        isSaving = recordType == RECORD_TYPE_SAVE,
    )

    private fun String?.toApiDate(): String? {
        val date = this?.substringBefore(' ')?.trim().orEmpty()
        return date.takeIf { API_DATE_REGEX.matches(it) }
    }

    private fun Calendar.toApiDate(): String = String.format(
        Locale.US,
        "%04d-%02d-%02d",
        get(Calendar.YEAR),
        get(Calendar.MONTH) + 1,
        get(Calendar.DAY_OF_MONTH),
    )

    companion object {
        fun createIntent(context: Context, planId: Int): Intent =
            Intent(context, SavingsCalendarActivity::class.java)
                .putExtra(EXTRA_PLAN_ID, planId)

        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val INVALID_PLAN_ID = -1
        private const val RECORD_TYPE_SAVE = 1
        private const val DAYS_IN_WEEK = 7
        private const val CALENDAR_CELL_COUNT = 42
        private val API_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val TRANSACTION_DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlanTransactionItem>() {
            override fun areItemsTheSame(
                oldItem: PlanTransactionItem,
                newItem: PlanTransactionItem,
            ): Boolean = oldItem.id != null && oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PlanTransactionItem,
                newItem: PlanTransactionItem,
            ): Boolean = oldItem == newItem
        }
    }
}
