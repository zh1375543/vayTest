package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.vaycore.finance.R
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.LoanProductSummaryViewBinding
import com.vaycore.finance.ui.extension.loadImage

/** Shared product and amount summary used by both single and multiple loan pages. */
class LoanProductSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding =
        LoanProductSummaryViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var detailsClickAction: (() -> Unit)? = null

    init {
        binding.tvDetails.setOnClickListener { detailsClickAction?.invoke() }
        binding.ivArrow.setOnClickListener { detailsClickAction?.invoke() }
    }

    fun bind(product: ProductBean, displayAmount: CharSequence) = with(binding) {
        tvProductName.text = product.productName
        ivProductIcon.loadImage(product.productImageUrl, R.mipmap.ic_product_defalut_img)
        tvLoanAmount.text = context.getString(R.string.loan_amount, product.currency)
        tvAmount.text = displayAmount
    }

    fun setExpanded(expanded: Boolean) {
        binding.ivArrow.rotation = if (expanded) 180f else 0f
    }

    fun setOnDetailsClickListener(action: () -> Unit) {
        detailsClickAction = action
    }
}
