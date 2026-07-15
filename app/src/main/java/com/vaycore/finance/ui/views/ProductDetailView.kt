package com.vaycore.finance.ui.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.databinding.ProductDetailViewBinding
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.util.isPositive
import com.vaycore.finance.util.formatAmount
import com.vaycore.finance.ui.adapters.ProductFeeAdapter
import com.vaycore.finance.ui.adapters.ProductInstallmentAdapter
import com.vaycore.finance.ui.adapters.ProductRepaymentMenuAdapter
import com.vaycore.finance.util.LogUtil

class ProductDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding =
        ProductDetailViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val feeAdapter by lazy { ProductFeeAdapter() }
    private val repaymentMenuAdapter by lazy { ProductRepaymentMenuAdapter() }
    private val installAdapter by lazy { ProductInstallmentAdapter() }

    private var currentProduct: ProductBean? = null

    // callbacks
    var onTermChanged: ((productId: Long?, termId: Long?) -> Unit)? = null
    var onInstallmentChanged: ((productId: Long?, planNum: Int?) -> Unit)? = null

    init {
        initView()
    }

    private fun initView() = with(binding) {
        rvFee.adapter = feeAdapter
        rvPlan.adapter = repaymentMenuAdapter
        rvRepayment.adapter = installAdapter

        ivMorePlan.setOnClickListener {
            repaymentGroup.isVisible = !repaymentGroup.isVisible
            ivMorePlan.rotation = if (repaymentGroup.isVisible) 180f else 0f
        }
        planLayout.isVisible = currentProduct?.isPlanLayoutVisible ?: false
        ivArrowPlan.rotation = if (planLayout.isVisible) 180f else 0f
        // write state back to bean on plan click
        repaymentMenuAdapter.setOnItemClickListener { item, position ->
            if (position == repaymentMenuAdapter.selectPosition) return@setOnItemClickListener
            repaymentMenuAdapter.selectPosition = position
            currentProduct?.selectedTermIndex = position  // write back to bean
            val productId = currentProduct?.id ?: currentProduct?.productId
            onTermChanged?.invoke(productId, item.id)
            updateUIByPlan(item)
            repaymentMenuAdapter.notifyItemRangeChanged(0, repaymentMenuAdapter.itemCount, 0)
        }

        ivArrowPlan.setOnClickListener {
            planLayout.isVisible = !planLayout.isVisible
            ivArrowPlan.rotation = if (planLayout.isVisible) 180f else 0f
            currentProduct?.isPlanLayoutVisible = planLayout.isVisible  // write back to bean
        }
    }

    // ================== Public methods ==================

    fun setData(product: ProductBean) {
        currentProduct = product

        binding.apply {
            val isVisible = product.isPlanLayoutVisible ?: false
            planLayout.isVisible = isVisible
            ivArrowPlan.rotation = if (isVisible) 180f else 0f

            tvInterestTitle.text =
                context.getString(R.string.interest_day, "${product.interestRate}%")

            tvInterest.text =
                product.interestAmount.formatAmount(product.currencySymbol)

            tvDays.text =
                context.getString(R.string.num_days, product.timeLimit.toString())
            tvActuallyTitle.text =
                String.format(
                    context.getString(R.string.actually_amount),
                    product.currencySymbol ?: ""
                ).replace("()", "")

            tvActuallyAmount.text =
                product.actualAmount.formatAmount(product.currencySymbol)
            tvModel.text = "${Build.BRAND} ${Build.MODEL}"

            tvDate.text = product.repayTimeStr

            feeAdapter.submitItems(product.appProductHandleFeeConfigDtos)

            planGroup.isVisible = !product.loanTermConfigDTOList.isNullOrEmpty()

            if (!product.loanTermConfigDTOList.isNullOrEmpty()) {
                handlePlan(product)
            } else {
                repaymentMenuAdapter.selectPosition = -1
                repaymentMenuAdapter.submitItems(null)
                planLayout.isVisible = false
            }
        }
    }

    private fun handlePlan(product: ProductBean) {
        val list = product.loanTermConfigDTOList ?: return
        LogUtil.e("selectIndex:${product.selectedTermIndex}")

        // if product.selectedTermIndex is null, it was never set (or GSON assigned null)
        val isFirst = product.selectedTermIndex == null
        
        val index = if (product.selectedTermIndex != null && product.selectedTermIndex!! >= 0 && product.selectedTermIndex!! < list.size) {
            product.selectedTermIndex!!
        } else {
            val defaultSignIndex = list.indexOfFirst { it.defaultSign == 1 }
            val isDefaultIndex = list.indexOfFirst { it.isDefault == 1 }

            val foundIndex = when {
                defaultSignIndex >= 0 -> defaultSignIndex
                isDefaultIndex >= 0 -> isDefaultIndex
                else -> 0
            }
            foundIndex.also { product.selectedTermIndex = it }
        }

        repaymentMenuAdapter.selectPosition = index
        repaymentMenuAdapter.submitItems(list)
        repaymentMenuAdapter.notifyDataSetChanged() // force refresh adapter's internal selection state
        binding.rvPlan.post { binding.rvPlan.scrollToPosition(index) }

        // only trigger callback on first load
        if (isFirst) {
            onTermChanged?.invoke(product.id ?: product.productId, list[index].id)
        }

        updateUIByPlan(list[index])
    }

    private fun updateUIByPlan(item: ProductBean) {
        binding.apply {

            tvInterestTitle.text =
                context.getString(R.string.interest_day, "${item.interestRate}%")

            tvInterest.text = item.interestAmount.formatAmount(currentProduct?.currencySymbol)

            tvDays.text =
                context.getString(R.string.num_days, item.timeLimit.toString())

            tvActuallyAmount.text =
                item.actualAmount.formatAmount(
                    item.currencySymbol ?: currentProduct?.currencySymbol
                )

            tvDate.text = item.repayTimeStr

            tvInstallFee.text =
                item.installmentServiceFee.formatAmount(item.currencySymbol)

            feeAdapter.submitItems(item.appProductHandleFeeConfigDtos)

            updateInstallment(item)
        }
    }

    private fun updateInstallment(item: ProductBean) {
        binding.apply {

            val productId = currentProduct?.id ?: currentProduct?.productId

            if (!item.productInstallmentPlanDTOList.isNullOrEmpty()) {
                val list = item.productInstallmentPlanDTOList
                val defaultSignIndex = list.indexOfFirst { it.defaultSign == 1 }
                val isDefaultIndex = list.indexOfFirst { it.isDefault == 1 }

                val index = when {
                    defaultSignIndex >= 0 -> defaultSignIndex
                    isDefaultIndex >= 0 -> isDefaultIndex
                    else -> 0
                }.coerceIn(0, list.lastIndex)

                val plan = list[index]

                installAdapter.submitItems(plan.appRepaymentPlanDTOList)

                // notify external listener
                onInstallmentChanged?.invoke(productId, plan.planNums)

                repaymentGroup.isVisible = true
                ivMorePlan.isVisible = true
                tvInstallDetail.isVisible = true
                installGroup.isVisible = item.installmentServiceFee.isPositive()

            } else {
                // also notify when no installments
                onInstallmentChanged?.invoke(productId, null)

                repaymentGroup.isVisible = false
                ivMorePlan.isVisible = false
                tvInstallDetail.isVisible = false
                installGroup.isVisible = false
            }
        }
    }
}
