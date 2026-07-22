package com.vaycore.finance.ui

import android.content.Context
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.databinding.YearMonthPickDialogBinding
import com.vaycore.finance.ui.extension.singleClick

fun Context.showYearMonthPickerDialog(
    selectedYear: Int,
    selectedMonth: Int,
    action: (year: Int, month: Int) -> Unit,
) {
    object : BaseSheetDialog<YearMonthPickDialogBinding>(
        this,
        YearMonthPickDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            yearMonthView.setSelectedYearMonth(selectedYear, selectedMonth)
            tvOk.singleClick {
                val year = yearMonthView.getSelectedYear()
                val month = yearMonthView.getSelectedMonth()
                dismiss()
                action(year, month)
            }
        }
    }.show()
}
