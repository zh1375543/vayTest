package com.vaycore.finance.ui

import android.app.Dialog
import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseDialog
import com.vaycore.finance.base.BaseSheetDialog
import com.vaycore.finance.data.local.LEASE_AGREEMENT
import com.vaycore.finance.data.local.PAWN_AGREEMENT
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.data.local.loginInfo
import com.vaycore.finance.databinding.CreditDialogBinding
import com.vaycore.finance.databinding.LoanAgreementDialogBinding
import com.vaycore.finance.databinding.NewProductDialogBinding
import com.vaycore.finance.ui.activities.WebViewActivity
import com.vaycore.finance.ui.adapters.ProductCardAdapter
import com.vaycore.finance.ui.extension.setClickableTextWithScale
import com.vaycore.finance.ui.extension.singleClick

fun Context.showLoanAgreementDialog(
    isTogether: Boolean = false,
    productId: String? = null,
    amount: String? = null,
    applyAction: () -> Unit,
) {
    object :
        BaseSheetDialog<LoanAgreementDialogBinding>(
            this,
            LoanAgreementDialogBinding::inflate
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
    return object : BaseDialog<NewProductDialogBinding>(
        this,
        NewProductDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            setCanceledOnTouchOutside(false)
            var shouldTrackClose = true
            val fullText = String.format(getString(R.string.home_product_num), list.size)
            tvTitle.setClickableTextWithScale(
                fullText,
                list.size.toString(),
                "#89F5C7".toColorInt()
            )
            rvProduct.adapter = ProductCardAdapter().apply {
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
    return object : BaseDialog<CreditDialogBinding>(
        this,
        CreditDialogBinding::inflate,
    ) {
        override fun initView() = with(binding) {
            super.initView()
            tvAmount.text = amount
            tvLater.singleClick { dismiss() }
            btnWithdraw.singleClick {
                dismiss()
                withdrawAction()
            }
        }
    }
}
