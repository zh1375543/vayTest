package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.vaycore.finance.databinding.SingleProductDetailViewBinding
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.ui.adapters.ProductInstallmentAdapter
import com.vaycore.finance.ui.adapters.ProductRepaymentMenuAdapter
import com.vaycore.finance.util.LogUtil

class SingleProductDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding =
        SingleProductDetailViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val repaymentMenuAdapter by lazy { ProductRepaymentMenuAdapter() }
    private val installAdapter by lazy { ProductInstallmentAdapter() }

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

        ivMorePlan.rotation = 180f
        ivMorePlan.setOnClickListener {
            repaymentGroup.isVisible = !repaymentGroup.isVisible
            ivMorePlan.rotation = if (repaymentGroup.isVisible) 180f else 0f
        }

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
                ivMorePlan.isVisible = true
                tvInstallDetail.isVisible = true
            } else {
                onInstallmentChanged?.invoke(productId, null)
                repaymentGroup.isVisible = false
                ivMorePlan.isVisible = false
                tvInstallDetail.isVisible = false
            }
        }
    }
}
