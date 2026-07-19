package com.vaycore.finance.ui.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.databinding.SingleProductDetailViewBinding
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.ui.adapters.ProductHeaderFeeAdapter
import com.vaycore.finance.ui.adapters.ProductInstallmentAdapter
import com.vaycore.finance.ui.adapters.ProductRepaymentMenuAdapter
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.formatAmount

class SingleProductDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding =
        SingleProductDetailViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val repaymentMenuAdapter by lazy { ProductRepaymentMenuAdapter() }
    private val installAdapter by lazy { ProductInstallmentAdapter() }
    private val headerFeeAdapter by lazy { ProductHeaderFeeAdapter() }

    private var currentProduct: ProductBean? = null

    var onTermChanged: ((productId: Long?, termId: Long?) -> Unit)? = null
    var onInstallmentChanged: ((productId: Long?, planNum: Int?) -> Unit)? = null
    var onPlanSelected: ((ProductBean) -> Unit)? = null

    init {
        initView()
    }

    private fun initView() = with(binding) {
        rvPlan.adapter = repaymentMenuAdapter
        rvRepayment.adapter = installAdapter
        rvHeaderFee.adapter = headerFeeAdapter

        ivMoreDetail.rotation = 180f
        val toggleDetails = {
            detailsGroup.isVisible = !detailsGroup.isVisible
            ivMoreDetail.rotation = if (detailsGroup.isVisible) 180f else 0f
        }
        ivMoreDetail.setOnClickListener { toggleDetails() }
        tvDetailTitle.setOnClickListener { toggleDetails() }

        ivMorePlan.rotation = 180f
        val togglePlans = {
            val isPlanVisible = !rvPlan.isVisible
            rvPlan.isVisible = isPlanVisible
            repaymentHeaderGroup.isVisible = isPlanVisible
            repaymentGroup.isVisible = isPlanVisible
            ivMorePlan.rotation = if (isPlanVisible) 180f else 0f
        }
        ivMorePlan.setOnClickListener { togglePlans() }
        tvInstallDetail.setOnClickListener { togglePlans() }

        ivMoreRepayment.rotation = 180f
        val toggleRepayment = {
            repaymentGroup.isVisible = !repaymentGroup.isVisible
            ivMoreRepayment.rotation = if (repaymentGroup.isVisible) 180f else 0f
        }
        ivMoreRepayment.setOnClickListener { toggleRepayment() }
        tvRepaymentDetail.setOnClickListener { toggleRepayment() }

        repaymentMenuAdapter.setOnItemClickListener { item, position ->
            if (position == repaymentMenuAdapter.selectPosition) return@setOnItemClickListener
            repaymentMenuAdapter.selectPosition = position
            currentProduct?.selectedTermIndex = position
            val productId = currentProduct?.id ?: currentProduct?.productId
            onTermChanged?.invoke(productId, item.id)
            updateUIByPlan(item)
            repaymentMenuAdapter.notifyItemRangeChanged(0, repaymentMenuAdapter.itemCount, 0)
        }
    }

    fun bindHeaderDetail(plan: ProductBean, currencySymbol: String?) = with(binding) {
        tvDays.text = context.getString(R.string.num_days, plan.timeLimit.toString())
        tvActuallyTitle.text =
            String.format(context.getString(R.string.actually_amount), currencySymbol ?: "").replace("()", "")
        tvActuallyAmount.text = plan.actualAmount.formatAmount(currencySymbol)
        tvInterestTitle.text = context.getString(R.string.interest_day, "${plan.interestRate}%")
        tvInterest.text = plan.interestAmount.formatAmount(currencySymbol)
        tvDate.text = plan.repayTimeStr
        tvInstallFee.text = plan.installmentServiceFee.formatAmount(plan.currencySymbol)
        tvModel.text = "${Build.BRAND} ${Build.MODEL}"
        headerFeeAdapter.submitItems(plan.appProductHandleFeeConfigDtos)
    }

    fun setData(product: ProductBean) {
        currentProduct = product

        val hasPlans = !product.loanTermConfigDTOList.isNullOrEmpty()
        binding.planGroup.isVisible = hasPlans
        binding.rvPlan.isVisible = hasPlans

        if (hasPlans) {
            handlePlan(product)
        } else {
            repaymentMenuAdapter.selectPosition = -1
            repaymentMenuAdapter.submitItems(null)
            binding.repaymentGroup.isVisible = false
            binding.repaymentHeaderGroup.isVisible = false
            binding.ivMorePlan.isVisible = false
            binding.tvInstallDetail.isVisible = false
        }
    }

    private fun handlePlan(product: ProductBean) {
        val list = product.loanTermConfigDTOList ?: return
        LogUtil.e("singleSelectIndex:${product.selectedTermIndex}")

        val isFirst = product.selectedTermIndex == null
        val index = if (
            product.selectedTermIndex != null &&
            product.selectedTermIndex!! >= 0 &&
            product.selectedTermIndex!! < list.size
        ) {
            product.selectedTermIndex!!
        } else {
            val defaultSignIndex = list.indexOfFirst { it.defaultSign == 1 }
            val isDefaultIndex = list.indexOfFirst { it.isDefault == 1 }
            when {
                defaultSignIndex >= 0 -> defaultSignIndex
                isDefaultIndex >= 0 -> isDefaultIndex
                else -> 0
            }.also { product.selectedTermIndex = it }
        }

        repaymentMenuAdapter.selectPosition = index
        repaymentMenuAdapter.submitItems(list)
        repaymentMenuAdapter.notifyDataSetChanged()
        binding.rvPlan.post { binding.rvPlan.scrollToPosition(index) }

        if (isFirst) {
            onTermChanged?.invoke(product.id ?: product.productId, list[index].id)
        }

        updateUIByPlan(list[index])
    }

    private fun updateUIByPlan(item: ProductBean) {
        onPlanSelected?.invoke(item)
        updateInstallment(item)
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
                onInstallmentChanged?.invoke(productId, plan.planNums)

                repaymentGroup.isVisible = true
                repaymentHeaderGroup.isVisible = true
                ivMorePlan.isVisible = true
                tvInstallDetail.isVisible = true
            } else {
                onInstallmentChanged?.invoke(productId, null)
                repaymentGroup.isVisible = false
                repaymentHeaderGroup.isVisible = false
                ivMorePlan.isVisible = false
                tvInstallDetail.isVisible = false
            }
        }
    }
}
