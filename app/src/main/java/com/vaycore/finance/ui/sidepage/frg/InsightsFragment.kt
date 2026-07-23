package com.vaycore.finance.ui.sidepage.frg

import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.sideBean.SavingsReportResponse
import com.vaycore.finance.databinding.SidepageStatsFragmentBinding
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding
import java.math.BigDecimal

/** Insights page without analytics hooks. */
class InsightsFragment : BaseFragment<SidepageStatsFragmentBinding>(
    R.layout.sidepage_stats_fragment
) {
    override val binding by viewBinding(SidepageStatsFragmentBinding::bind)
    private val viewModel by viewModels<SideHomeViewModel>()

    override fun initView() {
        bindStaticReport()
        viewModel.saveReport()
    }

    override fun initObserve() {
        viewModel.savingsReportResult.observe(viewLifecycleOwner) {
            renderSavingsReport(it)
        }
    }

    private fun bindStaticReport() = with(binding) {
        tvLoanAmount.text = BigDecimal.ZERO.formatAmountWithPrefix()
        tvSavedThisMonth.text = BigDecimal.ZERO.formatAmountWithPrefix()
        tvTotalSavings.text = getString(R.string.portal_savings_day_value, 0)

        totalPlansCard.tvMetricTitle.setText(R.string.portal_total_plans)
        totalPlansCard.tvMetricValue.text = "0"
        activePlansCard.tvMetricTitle.setText(R.string.portal_active_plans)
        activePlansCard.tvMetricValue.text = "0"
        completedPlansCard.tvMetricTitle.setText(R.string.portal_completed_plans)
        completedPlansCard.tvMetricValue.text = "0"
        averageSavingsCard.tvMetricTitle.setText(R.string.portal_average_savings)
        averageSavingsCard.tvMetricValue.text = BigDecimal.ZERO.formatAmountWithPrefix()
        tvLevelDesc.text = PLACEHOLDER
    }

    private fun renderSavingsReport(data: SavingsReportResponse?) = with(binding) {
        tvLoanAmount.text = data?.totalSavedAmount.formatAmountWithPrefix()
        tvSavedThisMonth.text = data?.monthSavedAmount.formatAmountWithPrefix()
        tvTotalSavings.text = getString(
            R.string.portal_savings_day_value,
            data?.savingDays ?: 0,
        )

        totalPlansCard.tvMetricValue.text = (data?.totalPlanCount ?: 0).toString()
        activePlansCard.tvMetricValue.text = (data?.processingPlanCount ?: 0).toString()
        completedPlansCard.tvMetricValue.text = (data?.finishedPlanCount ?: 0).toString()
        averageSavingsCard.tvMetricValue.text = data?.averageSaveAmount.formatAmountWithPrefix()
        tvLevelDesc.text = data?.levelText.orPlaceholder()
    }

    private companion object {
        const val PLACEHOLDER = "-"
    }

    private fun String?.orPlaceholder(): String = takeUnless { it.isNullOrBlank() } ?: PLACEHOLDER
}
