package com.vaycore.finance.ui

import android.app.Dialog
import android.content.Context
import androidx.core.view.isVisible
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.data.LEASE_AGREEMENT
import com.vaycore.finance.data.PAWN_AGREEMENT
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.data.loginInfo
import com.vaycore.finance.databinding.DialogAvailableCreditBinding
import com.vaycore.finance.databinding.DialogLoanAgreementBinding
import com.vaycore.finance.databinding.DialogLoanOfferPickerBinding
import com.vaycore.finance.browser.WebViewActivity
import com.vaycore.finance.loan.adapter.LoanOfferPickerAdapter
import com.vaycore.finance.ui.extension.singleClick

fun Context.showLoanAgreementDialog(
    isTogether: Boolean = false,
    productId: String? = null,
    amount: String? = null,
    applyAction: () -> Unit,
) {
    object :
        BaseDialog<DialogLoanAgreementBinding>(
            this,
            DialogLoanAgreementBinding::inflate
        ) {
        override fun initView() = with(binding) {
            super.initView()
            tvDesc.text = String.format(
                getString(R.string.agreement_confirmation_desc),
                BuildConfig.HTTP_HOST
            )
            tvPlease.isVisible = !isTogether
            tvLease.isVisible = !isTogether
            tvMortgage.isVisible = !isTogether
            btnApply.singleClick {
                dismiss()
                applyAction()
            }
            tvLease.singleClick {
                WebViewActivity.launch(
                    this@showLoanAgreementDialog, tvLease.text.toString(),
                    LEASE_AGREEMENT + "userId=${loginInfo?.id}&productId=${productId}&amount=${amount}"
                )
            }
            tvMortgage.singleClick {
                WebViewActivity.launch(
                    this@showLoanAgreementDialog, tvMortgage.text.toString(),
                    PAWN_AGREEMENT + "userId=${loginInfo?.id}&productId=${productId}&amount=${amount}"
                )
            }
        }
    }.show()
}



fun Context.createNewProductDialog(
    list: List<ProductBean>,
    closeAction: () -> Unit = {},
    action: () -> Unit,
): Dialog {
    return object : BaseDialog<DialogLoanOfferPickerBinding>(
        this,
        DialogLoanOfferPickerBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            window?.decorView?.setPadding(0, 0, 0, 0)
            var shouldTrackClose = true
            val fullText = String.format(getString(R.string.home_product_num), list.size)
            tvTitle.text = fullText
            rvProduct.adapter = LoanOfferPickerAdapter().apply {
                submitItems(list)
            }
            ivClose.singleClick {
                dismiss()
            }
            tvLoan.singleClick {
                shouldTrackClose = false
                dismiss()
                action.invoke()
            }
            setOnDismissListener {
                if (shouldTrackClose) closeAction.invoke()
            }
        }
    }
}

fun Context.createAvailableCreditDialog(
    amount: CharSequence,
    withdrawAction: () -> Unit,
): Dialog {
    return object : BaseDialog<DialogAvailableCreditBinding>(
        this,
        DialogAvailableCreditBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            window?.decorView?.setPadding(0, 0, 0, 0)
            tvAmount.text = amount
            tvLater.singleClick { dismiss() }
            btnWithdraw.singleClick {
                dismiss()
                withdrawAction()
            }
        }
    }
}
