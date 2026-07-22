package com.vaycore.finance.ui.sidepage.act

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.sideBean.PlanDetail
import com.vaycore.finance.data.local.sideBean.PlanDetailResponse
import com.vaycore.finance.data.local.sideBean.PlanRecord
import com.vaycore.finance.databinding.SidepagePlanDetailsActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.sidepage.adapter.PlanTransactionAdapter
import com.vaycore.finance.ui.sidepage.adapter.PlanTransactionItem
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.viewBinding
import java.util.Locale

/** Displays a savings plan summary and its transaction history. */
class PlanDetailsActivity : BaseActivity<SidepagePlanDetailsActivityBinding>() {

    override val binding by viewBinding(SidepagePlanDetailsActivityBinding::inflate)

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planId by lazy { intent.getIntExtra(EXTRA_PLAN_ID, INVALID_PLAN_ID) }
    private val transactionAdapter by lazy { PlanTransactionAdapter() }
    private var currentPlan: PlanDetail? = null
    private val savingsRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPlanDetail()
        }
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
        titleBar.setRightImage(R.mipmap.ic_at_date)
        titleBar.setRightImageAction {
            startActivity(Intent(this@PlanDetailsActivity, SavingsCalendarActivity::class.java))
        }
        btSaveMoney.singleClick {
            currentPlan?.let { plan ->
                savingsRecordLauncher.launch(
                    SavingsRecordActivity.createIntent(this@PlanDetailsActivity, plan, planId),
                )
            }
        }

        rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@PlanDetailsActivity)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
        loadingLayout.setOnRetryClickListener { loadPlanDetail() }
        loadPlanDetail()
    }

    override fun initObserve() = with(viewModel) {
        planDetailResult.observe(this@PlanDetailsActivity) { result ->
            renderPlanDetail(result)
        }
        planDetailFailed.observe(this@PlanDetailsActivity) { event ->
            if (event.getContentIfNotHandled() != null) {
                binding.loadingLayout.showError()
            }
        }
    }

    private fun loadPlanDetail() {
        if (planId == INVALID_PLAN_ID) {
            binding.loadingLayout.showError()
            return
        }
        binding.loadingLayout.showLoading()
        viewModel.getPlanDetail(planId)
    }

    private fun renderPlanDetail(data: PlanDetailResponse?) = with(binding) {
        val plan = data?.plan
        currentPlan = plan
        tvPlanName.text = plan?.planName.orEmpty()
        tvSavingsGoalAmount.text = plan?.targetAmount.formatAmountWithPrefix()
        tvSavedAmount.text = plan?.savedAmount.formatAmountWithPrefix()
        tvRemainingAmount.text = plan?.remainingAmount.formatAmountWithPrefix()
        progressPlan.progress = plan?.progressPercent?.toInt()?.coerceIn(0, 100) ?: 0
        renderPlanSchedule(plan)

        val records = data?.recordList.orEmpty()
        tvTransactionDetails.isVisible = records.isNotEmpty()
        transactionListContainer.isVisible = records.isNotEmpty()
        transactionAdapter.submitItems(records.map { record ->
            PlanTransactionItem(
                id = record.id,
                title = record.recordTypeText.orEmpty(),
                amount = record.amount.formatAmountWithPrefix(),
                date = record.occurTime.orEmpty(),
                address = record.locationText
                    ?.takeIf(String::isNotBlank)
                    ?: record.coordinateText(),
                isSaving = record.recordType == RECORD_TYPE_SAVE,
            )
        })
        loadingLayout.showContent()
    }

    private fun renderPlanSchedule(plan: PlanDetail?) = with(binding) {
        tvPlanStartDate.text = getString(
            R.string.portal_plan_start_date,
            plan?.startDate.orPlaceholder(),
        )
        tvPlanEndDate.text = getString(
            R.string.portal_plan_target_date,
            plan?.targetDate.orPlaceholder(),
        )

        val remainingDaysText = plan?.remainingDaysText
            ?: plan?.remainingDays?.let { getString(R.string.portal_day_count, it) }
        tvRemainingDaysValue.text = remainingDaysText.orPlaceholder()

        tvNextSavingTime.text = getString(
            R.string.portal_next_saving_time,
            plan?.nextSaveDate.orPlaceholder(),
        )

        tvNextSavingDaysValue.text = plan?.nextSaveDateText.orPlaceholder()
    }

    private fun String?.orPlaceholder(): String = takeUnless { it.isNullOrBlank() } ?: "-"

    private fun PlanRecord.coordinateText(): String? {
        val latitude = latitude ?: return null
        val longitude = longitude ?: return null
        return String.format(Locale.ENGLISH, "%.6f, %.6f", latitude, longitude)
    }

    companion object {
        const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val INVALID_PLAN_ID = -1
        private const val RECORD_TYPE_SAVE = 1
    }
}
