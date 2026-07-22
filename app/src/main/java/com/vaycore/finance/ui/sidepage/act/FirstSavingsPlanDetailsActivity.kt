package com.vaycore.finance.ui.sidepage.act

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.sideBean.CreatePlanRequest
import com.vaycore.finance.data.local.bean.SelectionOption
import com.vaycore.finance.databinding.SidepageFirstSavingsPlanDetailsActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showOptionPickerDialog
import com.vaycore.finance.ui.sidepage.PortalActivity
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.viewBinding

/** Completes the required fields for a savings plan created during first login. */
class FirstSavingsPlanDetailsActivity :
    BaseActivity<SidepageFirstSavingsPlanDetailsActivityBinding>() {

    override val binding by viewBinding(SidepageFirstSavingsPlanDetailsActivityBinding::inflate)
    private val viewModel by viewModels<SideHomeViewModel>()

    private val planName: String by lazy {
        intent.getStringExtra(EXTRA_PLAN_NAME)?.trim().orEmpty()
    }

    private var selectedFrequency: Int? = null

    private val frequencyOptions by lazy {
        listOf(
            SelectionOption(info = getString(R.string.portal_frequency_daily), id = FREQUENCY_DAILY),
            SelectionOption(info = getString(R.string.portal_frequency_weekly), id = FREQUENCY_WEEKLY),
            SelectionOption(info = getString(R.string.portal_frequency_monthly), id = FREQUENCY_MONTHLY),
            SelectionOption(info = getString(R.string.portal_frequency_yearly), id = FREQUENCY_YEARLY),
        )
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        applyBottomInset(bottomAction)
        titleBar.setNavigationAction(::finish)
        titleBar.setAction(action = ::skipPlanSetup)

        targetAmountView.getEditText().doAfterTextChanged { updateSubmitState() }
        eachAmountView.getEditText().doAfterTextChanged { updateSubmitState() }
        frequencyView.setOnClick { showFrequencyPicker() }
        btSubmit.singleClick { submitPlan() }
        updateSubmitState()
    }

    override fun initObserve() {
        super.initObserve()
        viewModel.addPlanResult.observe(this) { event ->
            event.getContentIfNotHandled() ?: return@observe
            getString(R.string.portal_plan_created_successfully).showToastMessage()
            startActivity(
                Intent(this@FirstSavingsPlanDetailsActivity, PortalActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
        }
    }

    private fun showFrequencyPicker() {
        showOptionPickerDialog(
            frequencyOptions.indexOfFirst { it.id == selectedFrequency },
            frequencyOptions,
        ) { position ->
            frequencyOptions.getOrNull(position)?.let { option ->
                selectedFrequency = option.id
                binding.frequencyView.setText(option.info)
                binding.frequencyView.hideError()
                updateSubmitState()
            }
        }
    }

    private fun updateSubmitState() = with(binding) {
        btSubmit.isEnabled = targetAmountView.getText().isNotBlank() &&
            selectedFrequency != null &&
            eachAmountView.getText().isNotBlank()
    }

    private fun submitPlan() = with(binding) {
        val targetAmount = targetAmountView.getText().toPlanAmountOrNull()
        val eachAmount = eachAmountView.getText().toPlanAmountOrNull()
        val invalid = when {
            targetAmountView.getText().isBlank() -> targetAmountView.apply { showError() }
            targetAmount == null || targetAmount <= 0 -> targetAmountView.apply {
                showError(getString(R.string.portal_amount_must_be_greater_than_zero))
            }
            selectedFrequency == null -> frequencyView.apply { showError() }
            eachAmountView.getText().isBlank() -> eachAmountView.apply { showError() }
            eachAmount == null || eachAmount <= 0 -> eachAmountView.apply {
                showError(getString(R.string.portal_amount_must_be_greater_than_zero))
            }
            eachAmount > targetAmount -> eachAmountView.apply {
                showError(getString(R.string.portal_each_amount_exceeds_target))
            }
            else -> null
        }
        if (invalid != null || planName.isBlank()) return@with

        viewModel.addPlan(
            CreatePlanRequest(
                planName = planName,
                targetAmount = targetAmount,
                frequencyType = selectedFrequency,
                eachAmount = eachAmount,
                planIcon = null,
            ),
        )
    }

    private fun String.toPlanAmountOrNull() = trim().replace(",", "").toIntOrNull()

    private fun skipPlanSetup() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_PLAN_NAME = "extra_plan_name"

        private const val FREQUENCY_DAILY = 1
        private const val FREQUENCY_WEEKLY = 2
        private const val FREQUENCY_MONTHLY = 3
        private const val FREQUENCY_YEARLY = 4
    }
}
