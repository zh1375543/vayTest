package com.vaycore.finance.ui.adapters

import androidx.core.view.isVisible
import android.view.View
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.bean.ProductBean
import com.vaycore.finance.databinding.ItemComboBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.formatAmount

class ComboAdapter(
    val productInstallmentMap: MutableMap<Long?, Int?> = HashMap(),
    val termMap: MutableMap<Long?, Long?> = HashMap(),
) :
    BaseAdapter<ProductBean, ItemComboBinding>(ItemComboBinding::inflate) {

    override fun bindItem(
        binding: ItemComboBinding,
        item: ProductBean,
        position: Int,
    ) = with(binding) {
        ivIcon.loadImage(item.productImageUrl, R.mipmap.product_icon)
        tvProductName.text = item.productName
        tvLoanAmount.text =
            String.format(context.getString(R.string.loan_amount), item.currency)
        tvAmount.text =
            if (item.canApply) item.maxLoanAmount.formatAmount(item.currencySymbol) else item.loanAmountRange
        detailView.isVisible = item.isExpand
        ivArrow.rotation = if (item.isExpand) 180f else 0f
        val expandClickListener = View.OnClickListener {
            item.isExpand = !item.isExpand
            notifyItemChanged(position)
        }
        tvDetails.setOnClickListener(expandClickListener)
        ivArrow.setOnClickListener(expandClickListener)
        detailView.apply {
            if (item.selectedTermIndex == null) {
                val defaultSignIndex =
                    item.loanTermConfigDTOList?.indexOfFirst { it1 -> it1.defaultSign == 1 }
                        ?: -1
                val isDefaultIndex =
                    item.loanTermConfigDTOList?.indexOfFirst { it1 -> it1.isDefault == 1 }
                        ?: -1
                item.selectedTermIndex = when {
                    defaultSignIndex >= 0 -> defaultSignIndex
                    isDefaultIndex >= 0 -> isDefaultIndex
                    else -> 0
                }
            }
            onTermChanged = { productId, termId ->
                termMap[productId] = termId
//                    LogUtil.e("onTermChanged:" + productId + "," + termId)
            }
            onInstallmentChanged = { productId, planNum ->
                productInstallmentMap[productId] = planNum
//                    LogUtil.e("onInstallmentChanged:" + productId + "," + planNum)
            }
//            LogUtil.e("isVis"+item.isPlanLayoutVisible)
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

        // keep termMap / productInstallmentMap, new data will trigger callback to overwrite
        submitItems(newItems)
    }
}
