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
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val editPlanLauncher = registerForActivityResult(
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
        planNameContainer.singleClick {
            currentPlan?.let { plan ->
                editPlanLauncher.launch(
                    EditPlanActivity.createIntent(this@PlanDetailsActivity, plan, planId),
                )
            }
        }
        ivUpdatePlan.singleClick {
            currentPlan?.let { plan ->
                editPlanLauncher.launch(
                    EditPlanActivity.createIntent(this@PlanDetailsActivity, plan, planId),
                )
            }
        }
        btSaveMoney.singleClick {
            currentPlan?.let { plan ->
                savingsRecordLauncher.launch(
                    SavingsRecordActivity.createIntent(this@PlanDetailsActivity, plan, planId),
                )
            }
        }
        btWithdraw.singleClick {
            currentPlan?.let { plan ->
                savingsRecordLauncher.launch(
                    SavingsRecordActivity.createIntent(
                        this@PlanDetailsActivity,
                        plan,
                        planId,
                        SavingsRecordActivity.RecordMode.WITHDRAW,
                    ),
                )
            }
        }

        rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@PlanDetailsActivity)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
        transactionAdapter.setOnItemClickListener { item, _ ->
            startActivity(
                PlanRecordDetailActivity.createIntent(
                    this@PlanDetailsActivity,
                    currentPlan,
                    item,
                ),
            )
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
        progressPlan.progress = plan.progressValue()
        renderPlanSchedule(plan)
        renderPlanState(plan)

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
                remark = record.remark,
                imageUrls = record.imageUrls,
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

    private fun renderPlanState(plan: PlanDetail?) = with(binding) {
        val isActive = plan?.status == STATUS_ACTIVE
        val statusText = plan?.statusText
            ?.takeIf(String::isNotBlank)
            ?: when (plan?.status) {
                STATUS_COMPLETED -> getString(R.string.portal_plan_status_completed)
                STATUS_CANCELLED -> getString(R.string.portal_plan_status_cancelled)
                else -> ""
            }

        planNameContainer.isEnabled = isActive
        planNameContainer.isClickable = isActive
        activePlanContentGroup.isVisible = isActive

        val shouldShowStatus = plan?.status == STATUS_COMPLETED || plan?.status == STATUS_CANCELLED
        tvPlanStatusLabel.isVisible = shouldShowStatus
        tvPlanStatusValue.isVisible = shouldShowStatus
        tvPlanStatusValue.text = statusText
        tvPlanStatusValue.setTextColor(
            getColor(
                if (plan?.status == STATUS_CANCELLED) R.color.C_FA560D else R.color.color_7087F8,
            ),
        )
    }

    private fun String?.orPlaceholder(): String = takeUnless { it.isNullOrBlank() } ?: "-"

    private fun PlanDetail?.progressValue(): Int {
        val plan = this ?: return 0
        plan.progressPercent?.let {
            return it.multiply(PROGRESS_SCALE)
                .setScale(0, RoundingMode.DOWN)
                .toInt()
                .coerceIn(0, PROGRESS_MAX)
        }
        val goal = plan.targetAmount ?: return 0
        if (goal <= BigDecimal.ZERO) return 0
        return (plan.savedAmount ?: BigDecimal.ZERO)
            .multiply(BigDecimal(100))
            .multiply(PROGRESS_SCALE)
            .divide(goal, 0, RoundingMode.DOWN)
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    private fun PlanRecord.coordinateText(): String? {
        val latitude = latitude ?: return null
        val longitude = longitude ?: return null
        return String.format(Locale.ENGLISH, "%.6f, %.6f", latitude, longitude)
    }

    companion object {
        const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val INVALID_PLAN_ID = -1
        private const val STATUS_ACTIVE = 1
        private const val STATUS_COMPLETED = 2
        private const val STATUS_CANCELLED = 3
        private const val RECORD_TYPE_SAVE = 1
        private const val PROGRESS_MAX = 10000
        private val PROGRESS_SCALE = BigDecimal(100)
    }
}
