package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.ItemComboBinding
import com.vaycore.finance.util.formatAmount

class ComboAdapter : BaseAdapter<ProductBean, ItemComboBinding>(ItemComboBinding::inflate) {

    override fun bindItem(
        binding: ItemComboBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        productSummaryView.bind(
            item,
            if (item.canApply) {
                item.maxLoanAmount.formatAmount(item.currencySymbol)
            } else {
                item.loanAmountRange.orEmpty()
            },
        )
        detailView.isVisible = item.isExpand
        productSummaryView.setExpanded(item.isExpand)
        productSummaryView.setOnDetailsClickListener {
            item.isExpand = !item.isExpand
            notifyItemChanged(position)
        }
        detailView.apply {
            if (item.selectedTermIndex == null) {
                val defaultSignIndex =
                    item.loanTermConfigDTOList?.indexOfFirst { it1 -> it1.defaultSign == 1 }
                        ?: -1
                item.selectedTermIndex = when {
                    defaultSignIndex >= 0 -> defaultSignIndex
                    else -> 0
                }
            }
            onPlanSelected = { plan ->
                bindHeaderDetail(plan, plan.currencySymbol ?: item.currencySymbol)
            }
            bindHeaderDetail(item, item.currencySymbol)
            setData(item)
        }
        Unit
    }

    fun submitItemsWithState(newItems: List<ProductBean>?) {
        val oldItems = items  // previous list

        newItems?.forEach { newItem ->
            val newId = newItem.id ?: newItem.productId
            // find the matching old item and merge UI state
            val oldItem = oldItems.find {
                (it.id ?: it.productId) == newId
            }
            if (oldItem != null) {
                newItem.isExpand = oldItem.isExpand
                newItem.selectedTermIndex = oldItem.selectedTermIndex
                newItem.isPlanLayoutVisible = oldItem.isPlanLayoutVisible
            }
        }
        submitItems(newItems)
    }
}
