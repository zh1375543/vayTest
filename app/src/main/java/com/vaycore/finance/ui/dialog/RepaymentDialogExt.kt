package com.vaycore.finance.ui

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.databinding.PaybackDialogBinding
import com.vaycore.finance.databinding.RepayAutoApplyDialogBinding
import com.vaycore.finance.ui.activities.MainActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.showToastMessage

fun Context.showRepayAndReapplyDialog(
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
