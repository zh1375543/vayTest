package com.vaycore.finance.payback

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.databinding.PaybackDialogBinding
import com.vaycore.finance.databinding.DialogRepayAndReapplyBinding
import com.vaycore.finance.app.MainActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.views.StatefulActionButton
import com.vaycore.finance.util.showToastMessage

fun Context.showRepayAndReapplyDialog(
    isDue: Boolean,
    isApplyAll: Boolean = false,
    closeAction: () -> Unit = {},
    confirmAction: () -> Unit,
) {
    object : BaseDialog<DialogRepayAndReapplyBinding>(
        this,
        DialogRepayAndReapplyBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setOnCancelListener { closeAction() }
            cbUnderstand.isSelected = true
            tvTitle.isVisible = !isApplyAll
            tvDesc.setText(
                if (isApplyAll) {
                    R.string.repay_auto_apply_all_dialog_desc
                } else {
                    R.string.repay_auto_apply_dialog_desc
                }
            )
            tvHint.text = this@showRepayAndReapplyDialog.createRepayHintText()
            btnApply.text = getString(
                if (isApplyAll) R.string.repay_auto_apply_all else R.string.repay_auto_apply
            )
            btnApply.updateAppearance(
                variant = StatefulActionButton.VARIANT_FILLED,
                solidColor = ContextCompat.getColor(
                    this@showRepayAndReapplyDialog,
                    when {
                        isApplyAll -> R.color.action_success
                        isDue -> R.color.status_error
                        else -> R.color.brand_primary
                    },
                ),
                textColor = ContextCompat.getColor(
                    this@showRepayAndReapplyDialog,
                    if (isApplyAll) R.color.text_primary else R.color.text_inverse,
                ),
            )
            cbUnderstand.singleClick {
                cbUnderstand.isSelected = !cbUnderstand.isSelected
            }
            tvUnderstand.singleClick {
                cbUnderstand.isSelected = !cbUnderstand.isSelected
            }
            btnApply.singleClick {
                if (!cbUnderstand.isSelected) {
                    getString(R.string.toast_repay_auto_apply_agreement).showToastMessage()
                    return@singleClick
                }
                dismiss()
                confirmAction()
            }
        }
    }.show()
}

private fun Context.createRepayHintText(): CharSequence {
    val hintText = getString(R.string.repay_auto_apply_dialog_hint)
    val repayText = getString(R.string.repay)
    val repayStart = hintText.lastIndexOf(repayText, ignoreCase = true)
    return SpannableString(hintText).apply {
        if (repayStart >= 0) {
            val repayEnd = repayStart + repayText.length
            setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(this@createRepayHintText, R.color.brand_primary)
                ),
                repayStart,
                repayEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                repayStart,
                repayEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
}

fun Context.createPaybackDialog(): Dialog {
    return object : BaseDialog<PaybackDialogBinding>(
        this,
        PaybackDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            window?.attributes?.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
            root.setOnClickListener { dismiss() }
            tvBorrow.singleClick {
                dismiss()
                MainActivity.launch(this@createPaybackDialog)
            }
        }
    }
}
