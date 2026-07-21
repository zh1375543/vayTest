package com.vaycore.finance.ui.sidepage.act

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.permission.PermissionLists
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.bean.CreatePlanRequest
import com.vaycore.finance.data.local.bean.SelectionOption
import com.vaycore.finance.databinding.SidepageAddPlanActivityBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.showOptionPickerDialog
import com.vaycore.finance.ui.viewmodels.PlanImageUploadState
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.compressImage
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.viewBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPlanActivity : BaseActivity<SidepageAddPlanActivityBinding>() {

    override val binding by viewBinding(SidepageAddPlanActivityBinding::inflate)
    private val viewModel by viewModels<SideHomeViewModel>()
    private var pendingCameraUri: Uri? = null

    private val planIconPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val sourceUri = result.data?.data
        sourceUri ?: return@registerForActivityResult
        processSelectedPlanIcon(sourceUri)
    }

    private val planIconCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val sourceUri = pendingCameraUri
        pendingCameraUri = null
        if (success && sourceUri != null) {
            processSelectedPlanIcon(sourceUri)
        }
    }

    private fun processSelectedPlanIcon(sourceUri: Uri) {
        lifecycleScope.launch {
            val compressedUri = withContext(Dispatchers.IO) {
                compressImage(sourceUri)
            } ?: return@launch
            selectedPlanIconUri = compressedUri
            binding.ivPlanIcon.apply {
                setPadding(0, 0, 0, 0)
                loadImage(compressedUri)
            }
            viewModel.uploadPlanImage(compressedUri)
        }
    }

    private val frequencyOptions by lazy {
        listOf(
            SelectionOption(
                id = FREQUENCY_DAILY,
                info = getString(R.string.portal_frequency_daily),
            ),
            SelectionOption(
                id = FREQUENCY_WEEKLY,
                info = getString(R.string.portal_frequency_weekly),
            ),
            SelectionOption(
                id = FREQUENCY_MONTHLY,
                info = getString(R.string.portal_frequency_monthly),
            ),
            SelectionOption(
                id = FREQUENCY_YEARLY,
                info = getString(R.string.portal_frequency_yearly),
            ),
        )
    }

    private var selectedFrequency: Int? = null
    private var selectedPlanIconUri: Uri? = null
    private var planIconUrl: String? = null
    private var isPlanIconUploadFailed = false

    override fun initView() = with(binding) {
        titleBar.setNavigationAction(::finish)
        setupBottomActionKeyboardBehavior()

        frequencyView.setOnClick {
            showOptionPickerDialog(
                frequencyOptions.indexOfFirst { it.id == selectedFrequency },
                frequencyOptions,
            ) { position ->
                frequencyOptions.getOrNull(position)?.let { option ->
                    selectedFrequency = option.id
                    frequencyView.setText(option.info)
                    frequencyView.hideError()
                }
            }
        }
        iconUploadView.singleClick { showPlanIconSourceDialog() }
        ivUploadStatus.singleClick {
            if (isPlanIconUploadFailed) retryPlanIconUpload()
        }

        btStartPlan.singleClick {
            submitPlan()
        }
    }

    private fun submitPlan() = with(binding) {
        if (!validateForm()) return@with

        viewModel.addPlan(
            CreatePlanRequest(
                planName = planNameView.getText().trim(),
                targetAmount = targetAmountView.getText().toPlanAmountOrNull(),
                frequencyType = selectedFrequency,
                eachAmount = eachAmountView.getText().toPlanAmountOrNull(),
                planIcon = planIconUrl,
            )
        )
    }

    private fun validateForm(): Boolean = with(binding) {
        val targetAmount = targetAmountView.getText().toPlanAmountOrNull()
        val eachAmount = eachAmountView.getText().toPlanAmountOrNull()
        val invalidView = when {
            planNameView.getText().isBlank() -> planNameView.apply { showError() }
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

        if (invalidView != null) {
            scrollToInvalidView(invalidView)
            return false
        }

        return true
    }

    private fun String.toPlanAmountOrNull() = trim()
        .replace(",", "")
        .toIntOrNull()

    private fun scrollToInvalidView(view: View) = with(binding) {
        view.requestFocus()
        scrollView.post {
            val topPadding = resources.getDimensionPixelSize(R.dimen.dp_12)
            scrollView.smoothScrollTo(0, (view.top - topPadding).coerceAtLeast(0))
        }
    }

    private fun setupBottomActionKeyboardBehavior() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionLayout) { _, insets ->
            binding.bottomActionLayout.isVisible =
                !insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomActionLayout)
    }

    override fun initObserve() {
        super.initObserve()
        viewModel.planImageUploadState.observe(this) { state ->
            when (state) {
                is PlanImageUploadState.Success -> {
                    planIconUrl = state.result.imageUrl
                    renderPlanIconResult(R.mipmap.ic_verify_kyc_success, false)
                }
                is PlanImageUploadState.Failed -> {
                    planIconUrl = null
                    renderPlanIconResult(R.mipmap.ic_verify_kyc_fail, true)
                }
            }
        }
        viewModel.addPlanResult.observe(this) { event ->
            event.getContentIfNotHandled() ?: return@observe
            getString(R.string.portal_plan_created_successfully).showToastMessage()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun openPlanIconPicker() {
        planIconPicker.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        )
    }

    private fun showPlanIconSourceDialog() {
        showConfirmDialog(
            title = getString(R.string.portal_choose_image_source),
            desc = "",
            cancel = getString(R.string.portal_choose_from_gallery),
            ok = getString(R.string.portal_take_photo),
            cancelAction = ::openPlanIconPicker,
            okAction = ::requestCameraPermission,
        )
    }

    private fun requestCameraPermission() {
        requestRuntimePermissions(
            array = arrayOf(PermissionLists.getCameraPermission()),
        ) {
            openPlanIconCamera()
        }
    }

    private fun openPlanIconCamera() {
        val outputFile = File(cacheDir, "plan_icon_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            outputFile,
        )
        pendingCameraUri = outputUri
        planIconCamera.launch(outputUri)
    }

    private fun retryPlanIconUpload() {
        selectedPlanIconUri?.let(viewModel::uploadPlanImage) ?: showPlanIconSourceDialog()
    }

    private fun renderPlanIconResult(statusIcon: Int, failed: Boolean) = with(binding) {
        isPlanIconUploadFailed = failed
        ivUploadStatus.isVisible = true
        ivUploadStatus.setImageResource(statusIcon)
    }

    private companion object {
        const val FREQUENCY_DAILY = 1
        const val FREQUENCY_WEEKLY = 2
        const val FREQUENCY_MONTHLY = 3
        const val FREQUENCY_YEARLY = 4
    }
}
