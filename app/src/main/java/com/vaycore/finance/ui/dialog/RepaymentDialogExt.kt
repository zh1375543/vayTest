package com.vaycore.finance.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.databinding.PaybackDialogBinding
import com.vaycore.finance.databinding.RepayAutoApplyDialogBinding
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.widget.ActionButtonView
import com.vaycore.finance.util.showToastMessage

fun Context.showRepayAndReapplyDialog(
    isDue: Boolean,
    isApplyAll: Boolean = false,
    closeAction: () -> Unit = {},
    confirmAction: () -> Unit,
) {
    object : BaseDialog<RepayAutoApplyDialogBinding>(
        this,
        RepayAutoApplyDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCanceledOnTouchOutside(false)
            setOnCancelListener { closeAction() }
            cbUnderstand.isSelected = true
            tvHint.text = this@showRepayAndReapplyDialog.createRepayHintText()
            btnApply.text = getString(
                if (isApplyAll) R.string.repay_auto_apply_all else R.string.repay_auto_apply
            )
            btnApply.applyStyle(
                variant = ActionButtonView.VARIANT_FILLED,
                solidColor = ContextCompat.getColor(
                    this@showRepayAndReapplyDialog,
                    when {
                        isApplyAll -> R.color.C_89F5C7
                        isDue -> R.color.C_F62909
                        else -> R.color.color_7087F8
                    },
                ),
                textColor = ContextCompat.getColor(
                    this@showRepayAndReapplyDialog,
                    if (isApplyAll) R.color.C_111827 else R.color.white,
                ),
            )
            cbUnderstand.singleClick {
                cbUnderstand.isSelected = !cbUnderstand.isSelected
            }
            tvUnderstand.singleClick {
                cbUnderstand.isSelected = !cbUnderstand.isSelected
            }
            btnClose.singleClick {
                dismiss()
                closeAction()
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
                    ContextCompat.getColor(this@createRepayHintText, R.color.color_FF8000)
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
