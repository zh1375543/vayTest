package com.vaycore.finance.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.view.isVisible
import com.google.android.play.core.review.ReviewManagerFactory
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.data.local.bean.CustomerContactConfig
import com.vaycore.finance.data.local.bean.GuestHomeResponse
import com.vaycore.finance.data.local.rateApp
import com.vaycore.finance.databinding.ConfirmDialogBinding
import com.vaycore.finance.databinding.ContactUsDialogBinding
import com.vaycore.finance.databinding.FeedDialogBinding
import com.vaycore.finance.databinding.HomeExpiredDialogBinding
import com.vaycore.finance.databinding.HomeRefuseDialogBinding
import com.vaycore.finance.databinding.LoadingDialogBinding
import com.vaycore.finance.databinding.RateDialogBinding
import com.vaycore.finance.databinding.VersionUpdateDialogBinding
import com.vaycore.finance.ui.adapters.ContactUsDialogAdapter
import com.vaycore.finance.ui.extension.hideKeyboard
import com.vaycore.finance.ui.extension.setSpannableClickableText
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.APP_UPGRADE
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.util.context.openPlayStore
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.util.toHtmlSpanned
import com.vaycore.finance.util.trackEvent

fun Context.createLoadingDialog(message: String = getString(R.string.loading)): Dialog {
    return object : BaseDialog<LoadingDialogBinding>(
        this@createLoadingDialog,
        LoadingDialogBinding::inflate
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvMessage.text = message
        }
    }.apply {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}

fun Context.showConfirmDialog(
    title: String = "",
    desc: String = getString(R.string.sure_logout),
    cancel: String = getString(R.string.closed),
    ok: String = getString(R.string.sure),
    highLight: String = "XXXXXXXXXXX",
    cancelAction: () -> Unit = {},
    okAction: () -> Unit,
) {
    object : BaseDialog<ConfirmDialogBinding>(
        this,
        ConfirmDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvTitle.isVisible = title.isNotBlank()
            tvTitle.text = title
            tvDesc.setSpannableClickableText(
                desc,
                highLight,
                getColor2(R.color.C_111827)
            ) {}
            tvDesc.isVisible = desc.isNotBlank()
            tvSure.text = ok
            tvClose.text = cancel
            tvClose.singleClick {
                dismiss()
                cancelAction()
            }
            tvSure.singleClick {
                dismiss()
                okAction.invoke()
            }
        }
    }.show()
}

fun Context.showContactUsDialog(homeBean: GuestHomeResponse) {
    object : BaseDialog<ContactUsDialogBinding>(
        this,
        ContactUsDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCanceledOnTouchOutside(false)
            val list = mutableListOf<CustomerContactConfig>()
            homeBean.customerPhone?.let { phone ->
                list.add(
                    CustomerContactConfig(
                        enTitle = "Phone Number",
                        vernacularTitle = "Số điện thoại",
                        content = phone,
                        buttonType = 2
                    )
                )
            }
            homeBean.customerEmail?.let { email ->
                list.add(
                    CustomerContactConfig(
                        enTitle = "Email",
                        vernacularTitle = "Email",
                        content = email,
                        buttonType = 1
                    )
                )
            }
            homeBean.customerConfigs?.let { configs ->
                list.addAll(configs)
            }
            rvCustomer.adapter = ContactUsDialogAdapter().apply {
                submitItems(list)
            }
            ivClose.singleClick {
                dismiss()
            }
        }
    }.show()
}



fun Context.createVersionUpdateDialog(): Dialog {
    return object : BaseDialog<VersionUpdateDialogBinding>(
        this@createVersionUpdateDialog,
        VersionUpdateDialogBinding::inflate
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            tvOK.singleClick {
                trackEvent(APP_UPGRADE)
                "https://play.google.com/store/apps/details?id=$packageName".openPlayStore()
            }
        }
    }
}

fun Context.showPreCreditExpiredDialog(date: String) {
    object : BaseDialog<HomeExpiredDialogBinding>(
        this,
        HomeExpiredDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCanceledOnTouchOutside(false)
            tvTips.setSpannableClickableText(
                String.format(getString(R.string.pre_credit_has_expired_tips), date),
                date.ifBlank { "XXXXXXXX" },
                getColor2(R.color.C_111827)
            ) {
            }
            ivClose.singleClick {
                dismiss()
            }
        }
    }.show()
}

fun Context.showCreditUnderReviewDialog() {
    object : BaseDialog<HomeRefuseDialogBinding>(
        this,
        HomeRefuseDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCanceledOnTouchOutside(false)
            ivClose.singleClick {
                dismiss()
            }
        }
    }.show()
}

fun Activity.showAppRatingDialog(action: (String) -> Unit) {
    if (rateApp) return
    rateApp = true
    object : BaseSheetDialog<RateDialogBinding>(this, RateDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            tvName.text = String.format(
                getString(R.string.rate_name),
                getString(R.string.app_name)
            ).toHtmlSpanned()
            tvButton.setOnClickListener {
                try {
                    val manager = ReviewManagerFactory.create(this@showAppRatingDialog)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(this@showAppRatingDialog, reviewInfo)
                            flow.addOnCompleteListener {
                                // don't do any
                            }
                        } else {
                            Log.e("InAppReview", "request failed", task.exception)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", e.message.toString())
                }
                dismiss()
            }
            tvNo.setOnClickListener {
                dismiss()
                showFeedbackDialog(action)
            }
        }
    }.show()
}

fun Activity.showFeedbackDialog(action: (String) -> Unit) {
    object : BaseDialog<FeedDialogBinding>(this, FeedDialogBinding::inflate) {
        override fun initView() = with(binding) {
            super.initView()
            window?.attributes?.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
            }
            root.setOnClickListener {
                etContent.hideKeyboard()
            }
            setCanceledOnTouchOutside(false)
            tvSubmit.setOnClickListener {
                if (etContent.text.isNullOrBlank()) {
                    getString(R.string.enter_feedback).showToastMessage()
                    return@setOnClickListener
                }
                action(etContent.text.toString())
                dismiss()
            }
        }
    }.show()
}
