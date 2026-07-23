package com.vaycore.finance.ui.sidepage.act

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.data.local.sideBean.PlanDetail
import com.vaycore.finance.data.local.sideBean.PlanItem
import com.vaycore.finance.data.local.sideBean.SavePlanRequest
import com.vaycore.finance.databinding.DialogRecordPhotoBinding
import com.vaycore.finance.databinding.DialogWithdrawPlanConfirmBinding
import com.vaycore.finance.databinding.SidepageSavingsRecordActivityBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.sidepage.adapter.RecordPhotoAdapter
import com.vaycore.finance.ui.viewmodels.PlanImageUploadState
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.compressImage
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.runtime.LocationInfoHelper
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.util.ArrayDeque
import java.util.Locale

/** Collects the amount, location, photo, and notes for a savings record. */
class SavingsRecordActivity : BaseActivity<SidepageSavingsRecordActivityBinding>() {

    override val binding by viewBinding(SidepageSavingsRecordActivityBinding::inflate)

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planId by lazy { intent.getIntExtra(EXTRA_PLAN_ID, INVALID_PLAN_ID) }
    private val recordMode by lazy {
        RecordMode.from(intent.getStringExtra(EXTRA_RECORD_MODE))
    }
    private val availableAmount by lazy {
        intent.getStringExtra(EXTRA_AVAILABLE_AMOUNT)?.toBigDecimalOrNull()
    }
    private val selectedPhotos = mutableListOf<Uri>()
    private val uploadedPhotoUrls = mutableMapOf<Uri, String>()
    private val photoUploadQueue = ArrayDeque<Uri>()
    private var pendingCameraUri: Uri? = null
    private var activePhotoUploadUri: Uri? = null
    private var photoDialog: PhotoManagerDialog? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private val recordPhotoAdapter by lazy {
        RecordPhotoAdapter(onPhotoClick = { showPhotoManagerDialog() })
    }

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoDialog?.addPhotos(uris)
        }
    }

    private val photoCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            photoDialog?.addPhotos(listOf(uri))
        }
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)
        applyBottomInset(bottomActionLayout)
        setupBottomActionKeyboardBehavior()
        bindPlanSummary()
        bindRecordMode()

        rvRecordPhotos.apply {
            layoutManager = GridLayoutManager(this@SavingsRecordActivity, MAX_PHOTO_COUNT)
            adapter = recordPhotoAdapter
        }

        photoUploadView.singleClick {
            showPhotoManagerDialog()
        }
        locationView.setOnClickListener { requestLocationPermission() }
        locationView.setOnClick(::requestLocationPermission)
        locationView.setEndIconClick(::requestLocationPermission)
        btSubmitRecord.singleClick { submitRecord() }
    }

    private fun bindPlanSummary() = with(binding) {
        tvPlanName.text = intent.getStringExtra(EXTRA_PLAN_NAME).orEmpty()
        tvPlannedSavingsAmount.text = intent
            .getStringExtra(EXTRA_PLANNED_SAVINGS)
            .orEmpty()

        val planIcon = intent.getStringExtra(EXTRA_PLAN_ICON)
        if (planIcon.isNullOrBlank()) {
            ivPlanIcon.setImageResource(R.mipmap.ic_product_defalut_img)
        } else {
            ivPlanIcon.loadImage(planIcon, R.mipmap.ic_product_defalut_img)
        }
        loadingLayout.showContent()
    }

    private fun bindRecordMode() = with(binding) {
        if (recordMode == RecordMode.SAVE) return@with

        titleBar.updateTitle(getString(R.string.portal_withdraw_record))
        tvPlannedSavingsLabel.setText(R.string.portal_saved_amount)
        savingAmountView.setTitle(getText(R.string.portal_withdraw_amount))
        locationView.setTitle(getString(R.string.portal_withdraw_location))
        tvRecordPhotoLabel.setText(R.string.portal_withdraw_photo)
        tvNotesLabel.setText(R.string.portal_withdraw_reason)
        btSubmitRecord.setText(R.string.portal_withdraw)
    }

    private fun setupBottomActionKeyboardBehavior() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionLayout) { _, insets ->
            binding.bottomActionLayout.isVisible =
                !insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomActionLayout)
    }

    private fun showPhotoManagerDialog() {
        if (photoDialog?.isShowing == true) return
        photoDialog = PhotoManagerDialog().also { it.show() }
    }

    private fun requestRecordPhotoCamera() {
        requestRuntimePermissions(
            array = arrayOf(PermissionLists.getCameraPermission()),
            isShowGuide = false,
        ) {
            val outputFile = File(cacheDir, "saving_record_${System.currentTimeMillis()}.jpg")
            val outputUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                outputFile,
            )
            pendingCameraUri = outputUri
            photoCamera.launch(outputUri)
        }
    }

    private fun renderRecordPhotoPreview() = with(binding) {
        val hasPhotos = selectedPhotos.isNotEmpty()
        rvRecordPhotos.isVisible = hasPhotos
        photoUploadView.isVisible = !hasPhotos
        recordPhotoAdapter.submitItems(selectedPhotos)
    }

    override fun initObserve() {
        super.initObserve()
        viewModel.planImageUploadState.observe(this) { state ->
            val sourceUri = activePhotoUploadUri ?: return@observe
            activePhotoUploadUri = null
            if (
                state is PlanImageUploadState.Success &&
                (selectedPhotos.contains(sourceUri) || photoDialog?.containsPhoto(sourceUri) == true)
            ) {
                uploadedPhotoUrls[sourceUri] = state.result.imageUrl
            }
            uploadNextRecordPhoto()
        }
        viewModel.savePlanResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { finishWithResult() }
        }
        viewModel.withdrawPlanResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { finishWithResult() }
        }
    }

    private fun enqueueRecordPhotoUploads(photos: List<Uri>) {
        photos
            .filterNot { it in uploadedPhotoUrls || it == activePhotoUploadUri || it in photoUploadQueue }
            .forEach(photoUploadQueue::addLast)
        uploadNextRecordPhoto()
    }

    private fun uploadNextRecordPhoto() {
        if (activePhotoUploadUri != null) return
        val sourceUri = photoUploadQueue.pollFirst() ?: return
        activePhotoUploadUri = sourceUri
        lifecycleScope.launch {
            val compressedUri = withContext(Dispatchers.IO) {
                compressImage(sourceUri)
            }
            if (compressedUri == null) {
                activePhotoUploadUri = null
                uploadNextRecordPhoto()
                return@launch
            }
            viewModel.uploadPlanImage(compressedUri)
        }
    }

    private fun submitRecord() = with(binding) {
        val amountText = savingAmountView.getText().trim().replace(",", "")
        val amount = amountText.toBigDecimalOrNull()
        when {
            amountText.isBlank() -> {
                savingAmountView.showError(
                    getString(
                        if (recordMode == RecordMode.WITHDRAW) {
                            R.string.portal_please_enter_withdraw_amount
                        } else {
                            R.string.portal_please_enter_saving_amount
                        },
                    ),
                )
                return@with
            }

            amount == null || amount <= BigDecimal.ZERO -> {
                savingAmountView.showError(getString(R.string.portal_amount_must_be_greater_than_zero))
                return@with
            }

            recordMode == RecordMode.WITHDRAW &&
                availableAmount != null && amount > availableAmount -> {
                savingAmountView.showError(getString(R.string.portal_withdraw_amount_exceeds_saved))
                return@with
            }
        }

        val request = SavePlanRequest(
            id = planId.toLong(),
            amount = amount.toDouble(),
            remark = etNotes.text?.toString()?.trim()?.takeIf(String::isNotBlank),
            imageUrls = selectedPhotos
                .mapNotNull(uploadedPhotoUrls::get)
                .joinToString(",")
                .takeIf(String::isNotBlank),
            locationText = locationView.getText().trim().takeIf(String::isNotBlank),
            latitude = currentLatitude,
            longitude = currentLongitude,
        )
        if (recordMode == RecordMode.WITHDRAW) {
            showWithdrawConfirmDialog(request)
        } else {
            viewModel.savePlan(request)
        }
    }

    private fun showWithdrawConfirmDialog(request: SavePlanRequest) {
        object : BaseDialog<DialogWithdrawPlanConfirmBinding>(
            this,
            DialogWithdrawPlanConfirmBinding::inflate,
        ) {
            override fun initView() = with(binding) {
                super.initView()
                btCancel.singleClick { dismiss() }
                btConfirm.singleClick {
                    dismiss()
                    viewModel.withdrawPlan(request)
                }
            }
        }.show()
    }

    private fun finishWithResult() {
        setResult(RESULT_OK)
        finish()
    }

    private fun requestLocationPermission() {
        requestRuntimePermissions(
            array = arrayOf(PermissionLists.getAccessCoarseLocationPermission()),
            refuseAction = { isNever, permissions ->
                if (isNever) {
                    showConfirmDialog(
                        title = String.format(
                            getString(R.string.dialog_permission_title),
                            getString(R.string.dialog_permission_location),
                        ),
                        desc = "",
                    ) {
                        XXPermissions.startPermissionActivity(this, permissions)
                    }
                }
            },
            isShowGuide = false,
        ) {
            loadCurrentLocation()
        }
    }

    private fun loadCurrentLocation() {
        lifecycleScope.launch {
            val location = LocationInfoHelper.getLocation()
            if (BuildConfig.DEBUG) {
                Log.d(
                    LOCATION_LOG_TAG,
                    "location=$location",
                )
            }
            val locationText = location?.let {
                String.format(Locale.ENGLISH, "%.6f, %.6f", it.latitude, it.longitude)
            }
                ?: return@launch
            currentLatitude = location.latitude
            currentLongitude = location.longitude
            binding.locationView.setText(locationText)
        }
    }

    private inner class PhotoManagerDialog : BaseDialog<DialogRecordPhotoBinding>(
        this@SavingsRecordActivity,
        DialogRecordPhotoBinding::inflate,
    ) {
        private val draftPhotos = selectedPhotos.toMutableList()
        private val photoAdapter: RecordPhotoAdapter = RecordPhotoAdapter(onDelete = { photo ->
            draftPhotos.remove(photo)
            renderPhotos()
        })

        override fun initView() = with(binding) {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            rvPhotos.layoutManager = GridLayoutManager(this@SavingsRecordActivity, MAX_PHOTO_COUNT)
            rvPhotos.adapter = photoAdapter

            tvCancel.singleClick { dismiss() }
            tvConfirm.singleClick {
                selectedPhotos.clear()
                selectedPhotos.addAll(draftPhotos)
                uploadedPhotoUrls.keys.retainAll(selectedPhotos.toSet())
                renderRecordPhotoPreview()
                enqueueRecordPhotoUploads(selectedPhotos)
                dismiss()
            }
            btGallery.singleClick { photoPicker.launch("image/*") }
            btCamera.singleClick { requestRecordPhotoCamera() }
            setOnDismissListener {
                if (photoDialog === this@PhotoManagerDialog) photoDialog = null
            }
            renderPhotos()
        }

        fun addPhotos(photos: List<Uri>) {
            val newPhotos = mutableListOf<Uri>()
            photos.forEach { photo ->
                if (draftPhotos.size < MAX_PHOTO_COUNT && photo !in draftPhotos) {
                    draftPhotos.add(photo)
                    newPhotos.add(photo)
                }
            }
            enqueueRecordPhotoUploads(newPhotos)
            renderPhotos()
        }

        fun containsPhoto(photo: Uri): Boolean = photo in draftPhotos

        private fun renderPhotos(): Unit = with(binding) {
            val hasPhotos = draftPhotos.isNotEmpty()
            rvPhotos.isVisible = hasPhotos
            tvEmpty.isVisible = !hasPhotos
            tvPhotoCount.isVisible = hasPhotos
            photoAdapter.submitItems(draftPhotos)

            if (hasPhotos) {
                val countText = getString(
                    R.string.portal_added_photos,
                    draftPhotos.size,
                    MAX_PHOTO_COUNT,
                )
                val highlightedText = "${draftPhotos.size}/$MAX_PHOTO_COUNT"
                val start = countText.indexOf(highlightedText)
                tvPhotoCount.text = SpannableString(countText).apply {
                    if (start >= 0) {
                        setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    this@SavingsRecordActivity,
                                    R.color.color_7087F8,
                                ),
                            ),
                            start,
                            start + highlightedText.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PLAN_ID = "extra_plan_id"
        private const val EXTRA_PLAN_NAME = "extra_plan_name"
        private const val EXTRA_PLAN_ICON = "extra_plan_icon"
        private const val EXTRA_PLANNED_SAVINGS = "extra_planned_savings"
        private const val EXTRA_RECORD_MODE = "extra_record_mode"
        private const val EXTRA_AVAILABLE_AMOUNT = "extra_available_amount"
        private const val INVALID_PLAN_ID = -1
        private const val LOCATION_LOG_TAG = "SavingsRecordLocation"
        private const val MAX_PHOTO_COUNT = 3

        fun createIntent(
            context: Context,
            plan: PlanDetail,
            fallbackPlanId: Int,
            recordMode: RecordMode = RecordMode.SAVE,
        ): Intent {
            return createIntent(
                context = context,
                planId = plan.id ?: fallbackPlanId,
                planName = plan.planName,
                planIcon = plan.planIcon,
                targetAmount = plan.targetAmount,
                savedAmount = plan.savedAmount,
                recordMode = recordMode,
            )
        }

        fun createIntent(
            context: Context,
            plan: PlanItem,
            recordMode: RecordMode = RecordMode.SAVE,
        ): Intent {
            return createIntent(
                context = context,
                planId = plan.id ?: INVALID_PLAN_ID,
                planName = plan.planName,
                planIcon = plan.planIcon,
                targetAmount = plan.targetAmount,
                savedAmount = plan.savedAmount,
                recordMode = recordMode,
            )
        }

        private fun createIntent(
            context: Context,
            planId: Int,
            planName: String?,
            planIcon: String?,
            targetAmount: BigDecimal?,
            savedAmount: BigDecimal?,
            recordMode: RecordMode,
        ): Intent {
            val plannedSavings = if (recordMode == RecordMode.WITHDRAW) {
                savedAmount
            } else {
                targetAmount ?: savedAmount
            }
            return Intent(context, SavingsRecordActivity::class.java).apply {
                putExtra(EXTRA_PLAN_ID, planId)
                putExtra(EXTRA_PLAN_NAME, planName)
                putExtra(EXTRA_PLAN_ICON, planIcon)
                putExtra(
                    EXTRA_PLANNED_SAVINGS,
                    plannedSavings?.formatAmountWithPrefix().orEmpty(),
                )
                putExtra(EXTRA_RECORD_MODE, recordMode.name)
                putExtra(EXTRA_AVAILABLE_AMOUNT, savedAmount?.toPlainString())
            }
        }
    }

    enum class RecordMode {
        SAVE,
        WITHDRAW;

        companion object {
            fun from(value: String?): RecordMode = entries.firstOrNull { it.name == value } ?: SAVE
        }
    }
}
