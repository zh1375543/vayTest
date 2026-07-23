package com.vaycore.finance.ui.sidepage.act

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.sideBean.PlanDetail
import com.vaycore.finance.databinding.SidepagePlanRecordDetailActivityBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.sidepage.adapter.PlanTransactionItem
import com.vaycore.finance.ui.sidepage.adapter.RecordPhotoAdapter
import com.vaycore.finance.util.viewBinding

/** Read-only details for a saving or withdrawal record. */
class PlanRecordDetailActivity : BaseActivity<SidepagePlanRecordDetailActivityBinding>() {

    override val binding by viewBinding(SidepagePlanRecordDetailActivityBinding::inflate)

    private val recordType by lazy { intent.getIntExtra(EXTRA_RECORD_TYPE, RECORD_TYPE_SAVING) }
    private val recordId by lazy { intent.getIntExtra(EXTRA_RECORD_ID, INVALID_RECORD_ID) }
    private val photoAdapter by lazy { RecordPhotoAdapter() }

    override fun initView() = with(binding) {
        applyTopInset(root)
        titleBar.setNavigationAction(::finish)

        tvPlanName.text = intent.getStringExtra(EXTRA_PLAN_NAME).orEmpty()

        val amount = intent.getStringExtra(EXTRA_AMOUNT)
        val displayAmount = amount.takeUnless { it.isNullOrBlank() } ?: "-"
        tvAmount.text = displayAmount
        savingAmountView.setText(displayAmount)

        val recordTime = intent.getStringExtra(EXTRA_RECORD_TIME)
        tvRecordTimeLabel.isVisible = !recordTime.isNullOrBlank()
        tvRecordTime.isVisible = !recordTime.isNullOrBlank()
        tvRecordTime.text = recordTime.orEmpty()

        val location = intent.getStringExtra(EXTRA_LOCATION)
        locationView.isVisible = !location.isNullOrBlank()
        locationView.setText(location.orEmpty())

        val remark = intent.getStringExtra(EXTRA_REMARK)
        notesView.isVisible = !remark.isNullOrBlank()
        notesView.setText(remark.orEmpty())

        val planIcon = intent.getStringExtra(EXTRA_PLAN_ICON)
        if (planIcon.isNullOrBlank()) {
            ivPlanIcon.setImageResource(R.mipmap.ic_product_defalut_img)
        } else {
            ivPlanIcon.loadImage(planIcon, R.mipmap.ic_product_defalut_img)
        }

        val photoUris = intent.getStringExtra(EXTRA_IMAGE_URLS)
            ?.split(',')
            ?.mapNotNull { it.trim().takeIf(String::isNotBlank)?.let(Uri::parse) }
            .orEmpty()
        rvRecordPhotos.apply {
            layoutManager = GridLayoutManager(this@PlanRecordDetailActivity, MAX_PHOTO_COUNT)
            adapter = photoAdapter
            isVisible = photoUris.isNotEmpty()
        }
        tvRecordPhotoLabel.isVisible = photoUris.isNotEmpty()
        photoAdapter.submitItems(photoUris)

        savingAmountView.setEnableEdit(false)
        locationView.setEnableEdit(false)
        notesView.setEnableEdit(false)
        bindRecordLabels()
        loadingLayout.showContent()
    }

    private fun bindRecordLabels() = with(binding) {
        val isSaving = recordType == RECORD_TYPE_SAVING
        titleBar.updateTitle(
            getString(
                if (isSaving) R.string.portal_saving_record_detail
                else R.string.portal_withdrawal_record_detail,
            ),
        )
        savingAmountView.setTitle(
            getText(if (isSaving) R.string.portal_today_saving else R.string.portal_withdraw_amount),
        )
        tvAmountLabel.setText(
            if (isSaving) R.string.portal_amount_saved else R.string.portal_amount_withdrawn,
        )
        tvRecordTimeLabel.setText(
            if (isSaving) R.string.portal_saving_time else R.string.portal_withdrawal_time,
        )
        locationView.setTitle(
            getString(
                if (isSaving) R.string.portal_savings_location
                else R.string.portal_withdrawal_location,
            ),
        )
    }

    companion object {
        private const val EXTRA_RECORD_TYPE = "extra_record_type"
        private const val EXTRA_RECORD_ID = "extra_record_id"
        private const val EXTRA_PLAN_NAME = "extra_plan_name"
        private const val EXTRA_PLAN_ICON = "extra_plan_icon"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_RECORD_TIME = "extra_record_time"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_REMARK = "extra_remark"
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val RECORD_TYPE_SAVING = 1
        private const val RECORD_TYPE_WITHDRAWAL = 2
        private const val INVALID_RECORD_ID = -1
        private const val MAX_PHOTO_COUNT = 3

        fun createIntent(
            context: Context,
            plan: PlanDetail?,
            record: PlanTransactionItem,
        ): Intent {
            return Intent(context, PlanRecordDetailActivity::class.java).apply {
                putExtra(
                    EXTRA_RECORD_TYPE,
                    if (record.isSaving) RECORD_TYPE_SAVING else RECORD_TYPE_WITHDRAWAL,
                )
                putExtra(EXTRA_RECORD_ID, record.id ?: INVALID_RECORD_ID)
                putExtra(EXTRA_PLAN_NAME, plan?.planName)
                putExtra(EXTRA_PLAN_ICON, plan?.planIcon)
                putExtra(EXTRA_AMOUNT, record.amount)
                putExtra(EXTRA_RECORD_TIME, record.date)
                putExtra(EXTRA_LOCATION, record.address)
                putExtra(EXTRA_REMARK, record.remark)
                putExtra(EXTRA_IMAGE_URLS, record.imageUrls)
            }
        }
    }

}
