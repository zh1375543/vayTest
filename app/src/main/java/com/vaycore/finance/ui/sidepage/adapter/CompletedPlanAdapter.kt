package com.vaycore.finance.ui.sidepage.adapter

import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.sideBean.PlanItem
import com.vaycore.finance.databinding.ItemSidepageCompletedPlanBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.formatAmountWithPrefix

/** Displays completed plans using their completion summary. */
class CompletedPlanAdapter :
    BaseAdapter<PlanItem, ItemSidepageCompletedPlanBinding>(ItemSidepageCompletedPlanBinding::inflate) {

    var onViewDetails: ((PlanItem) -> Unit)? = null

    override fun bindItem(
        binding: ItemSidepageCompletedPlanBinding,
        item: PlanItem,
        position: Int,
    ) = with(binding) {
        tvPlanName.text = item.planName.orEmpty()
        tvCompletionStatus.text = item.statusText
            ?.takeIf(String::isNotBlank)
            ?: context.getString(R.string.portal_plan_completed_successfully)
        tvSavedAmount.text = item.savedAmount.formatAmountWithPrefix()
        tvGoalAmount.text = item.targetAmount.formatAmountWithPrefix()
        tvTargetCompletionTime.text = item.targetDate.orEmpty()
        tvActualCompletionTime.text = item.finishTime.orEmpty()
        if (item.planIcon.isNullOrBlank()) {
            ivPlanIcon.setImageResource(R.mipmap.ic_product_defalut_img)
        } else {
            ivPlanIcon.loadImage(item.planIcon, R.mipmap.ic_product_defalut_img)
        }
        btViewDetails.singleClick { onViewDetails?.invoke(item) }
    }
}
