package com.vaycore.finance.ui.sidepage.act

import android.content.Context
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
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.data.local.sideBean.CancelPlanRequest
import com.vaycore.finance.data.local.sideBean.PlanDetail
import com.vaycore.finance.data.local.sideBean.UpdatePlanRequest
import com.vaycore.finance.databinding.DialogAbandonPlanBinding
import com.vaycore.finance.databinding.SidepageEditPlanActivityBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
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

/** Allows users to rename a savings plan and replace or remove its photo. */
class EditPlanActivity : BaseActivity<SidepageEditPlanActivityBinding>() {

    override val binding by viewBinding(SidepageEditPlanActivityBinding::inflate)

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planId by lazy { intent.getIntExtra(EXTRA_PLAN_ID, INVALID_PLAN_ID) }
    private val planStatus by lazy { intent.getIntExtra(EXTRA_PLAN_STATUS, INVALID_PLAN_STATUS) }
    private var planIconUrl: String? = null
    private var selectedPlanPhotoUri: Uri? = null
    private var activePhotoUploadUri: Uri? = null
    private var pendingCameraUri: Uri? = null
    private var isPlanPhotoUploadFailed = false

    private val planPhotoPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        result.data?.data?.let(::processSelectedPlanPhoto)
    }

    private val planPhotoCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val sourceUri = pendingCameraUri
        pendingCameraUri = null
        if (success && sourceUri != null) {
            processSelectedPlanPhoto(sourceUri)
        }
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
        applyBottomInset(bottomActionLayout)
        setupBottomActionKeyboardBehavior()
        bindInitialPlan()

        photoUploadView.singleClick { showPlanPhotoSourceDialog() }
        ivDeletePhoto.singleClick { clearPlanPhoto() }
        btEditPlan.singleClick { submitPlanUpdate() }
        btAbandonPlan.singleClick { showAbandonPlanDialog() }
    }

    override fun initObserve() {
        super.initObserve()
        with(viewModel) {
            planImageUploadState.observe(this@EditPlanActivity) { state ->
                val sourceUri = activePhotoUploadUri ?: return@observe
                activePhotoUploadUri = null
                if (sourceUri != selectedPlanPhotoUri) return@observe

                when (state) {
                    is PlanImageUploadState.Success -> {
                        planIconUrl = state.result.imageUrl
                        isPlanPhotoUploadFailed = false
                    }
                    is PlanImageUploadState.Failed -> {
                        planIconUrl = null
                        isPlanPhotoUploadFailed = true
                        getString(R.string.portal_photo_upload_failed).showToastMessage()
                    }
                }
            }
            updatePlanResult.observe(this@EditPlanActivity) { event ->
                event.getContentIfNotHandled() ?: return@observe
                getString(R.string.portal_plan_updated_successfully).showToastMessage()
                finishWithResult()
            }
            cancelPlanResult.observe(this@EditPlanActivity) { event ->
                event.getContentIfNotHandled() ?: return@observe
                getString(R.string.portal_plan_abandoned_successfully).showToastMessage()
                finishWithResult()
            }
        }
    }

    private fun bindInitialPlan() = with(binding) {
        planNameView.setText(intent.getStringExtra(EXTRA_PLAN_NAME).orEmpty())
        planIconUrl = intent.getStringExtra(EXTRA_PLAN_ICON)
        renderPlanPhoto()
    }

    private fun setupBottomActionKeyboardBehavior() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionLayout) { _, insets ->
            binding.bottomActionLayout.isVisible =
                !insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomActionLayout)
    }

    private fun submitPlanUpdate() = with(binding) {
        val planName = planNameView.getText().trim()
        if (planName.isBlank()) {
            planNameView.showError()
            scrollToInvalidView(planNameView)
            return@with
        }
        if (!canSubmitPhoto()) return@with

        viewModel.updatePlan(
            UpdatePlanRequest(
                id = planId.toLong(),
                planName = planName,
                planIcon = planIconUrl,
            ),
        )
    }

    private fun canSubmitPhoto(): Boolean {
        if (activePhotoUploadUri != null) {
            getString(R.string.portal_photo_uploading).showToastMessage()
            return false
        }
        if (selectedPlanPhotoUri != null && isPlanPhotoUploadFailed) {
            getString(R.string.portal_photo_upload_failed).showToastMessage()
            return false
        }
        return true
    }

    private fun scrollToInvalidView(view: View) = with(binding) {
        view.requestFocus()
        scrollView.post {
            val topPadding = resources.getDimensionPixelSize(R.dimen.dp_12)
            scrollView.smoothScrollTo(0, (view.top - topPadding).coerceAtLeast(0))
        }
    }

    private fun showPlanPhotoSourceDialog() {
        showConfirmDialog(
            title = getString(R.string.portal_choose_image_source),
            desc = "",
            cancel = getString(R.string.portal_choose_from_gallery),
            ok = getString(R.string.portal_take_photo),
            cancelAction = ::openPlanPhotoPicker,
            okAction = ::requestCameraPermission,
        )
    }

    private fun openPlanPhotoPicker() {
        planPhotoPicker.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            },
        )
    }

    private fun requestCameraPermission() {
        requestRuntimePermissions(
            array = arrayOf(PermissionLists.getCameraPermission()),
        ) {
            openPlanPhotoCamera()
        }
    }

    private fun openPlanPhotoCamera() {
        val outputFile = File(cacheDir, "edit_plan_${System.currentTimeMillis()}.jpg")
        val outputUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            outputFile,
        )
        pendingCameraUri = outputUri
        planPhotoCamera.launch(outputUri)
    }

    private fun processSelectedPlanPhoto(sourceUri: Uri) {
        lifecycleScope.launch {
            val compressedUri = withContext(Dispatchers.IO) {
                compressImage(sourceUri)
            } ?: return@launch
            selectedPlanPhotoUri = compressedUri
            activePhotoUploadUri = compressedUri
            planIconUrl = null
            isPlanPhotoUploadFailed = false
            renderPlanPhoto()
            viewModel.uploadPlanImage(compressedUri)
        }
    }

    private fun clearPlanPhoto() {
        selectedPlanPhotoUri = null
        activePhotoUploadUri = null
        planIconUrl = null
        isPlanPhotoUploadFailed = false
        renderPlanPhoto()
    }

    private fun renderPlanPhoto() = with(binding) {
        val localPhoto = selectedPlanPhotoUri
        val remotePhoto = planIconUrl
        val hasPhoto = localPhoto != null || !remotePhoto.isNullOrBlank()
        photoPreviewContainer.isVisible = hasPhoto

        when {
            localPhoto != null -> ivPlanPhoto.loadImage(localPhoto, R.mipmap.ic_as_defalut)
            !remotePhoto.isNullOrBlank() -> ivPlanPhoto.loadImage(remotePhoto, R.mipmap.ic_as_defalut)
        }
    }

    private fun showAbandonPlanDialog() {
        if (planStatus != STATUS_CAN_CANCEL) {
            getString(R.string.portal_plan_cannot_cancel).showToastMessage()
            return
        }

        object : BaseDialog<DialogAbandonPlanBinding>(
            this,
            DialogAbandonPlanBinding::inflate,
        ) {
            override fun initView() = with(binding) {
                super.initView()
                btThinkAgain.singleClick { dismiss() }
                btConfirmAbandon.singleClick {
                    dismiss()
                    viewModel.cancelPlan(CancelPlanRequest(id = planId.toLong()))
                }
            }
        }.show()
    }

    private fun finishWithResult() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_PLAN_NAME = "extra_plan_name"
        private const val EXTRA_PLAN_ICON = "extra_plan_icon"
        private const val EXTRA_PLAN_STATUS = "extra_plan_status"
        private const val INVALID_PLAN_ID = -1
        private const val INVALID_PLAN_STATUS = -1
        private const val STATUS_CAN_CANCEL = 1

        fun createIntent(
            context: Context,
            plan: PlanDetail,
            fallbackPlanId: Int,
        ): Intent {
            return Intent(context, EditPlanActivity::class.java).apply {
                putExtra(EXTRA_PLAN_ID, plan.id ?: fallbackPlanId)
                putExtra(EXTRA_PLAN_NAME, plan.planName)
                putExtra(EXTRA_PLAN_ICON, plan.planIcon)
                putExtra(EXTRA_PLAN_STATUS, plan.status ?: INVALID_PLAN_STATUS)
            }
        }
    }
}
