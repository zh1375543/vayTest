package com.vaycore.finance.ui.sidepage.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.sideBean.PlanItem
import com.vaycore.finance.databinding.ItemSidepageCancelledPlanBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.formatAmountWithPrefix

/** Displays cancelled plans with their saved amount and cancellation time. */
class CancelledPlanAdapter :
    BaseAdapter<PlanItem, ItemSidepageCancelledPlanBinding>(ItemSidepageCancelledPlanBinding::inflate) {

    var onViewDetails: ((PlanItem) -> Unit)? = null

    override fun bindItem(
        binding: ItemSidepageCancelledPlanBinding,
        item: PlanItem,
        position: Int,
    ) = with(binding) {
        tvPlanName.text = item.planName.orEmpty()
        tvCancelledTime.text = context.getString(
            R.string.portal_cancelled_time,
            item.cancelTime.orEmpty(),
        )
        tvSavedAmount.text = item.savedAmount.formatAmountWithPrefix()
        tvGoalAmount.text = item.targetAmount.formatAmountWithPrefix()
        if (item.planIcon.isNullOrBlank()) {
            ivPlanIcon.setImageResource(R.mipmap.ic_product_defalut_img)
        } else {
            ivPlanIcon.loadImage(item.planIcon, R.mipmap.ic_product_defalut_img)
        }
        btViewDetails.singleClick { onViewDetails?.invoke(item) }
    }
}
