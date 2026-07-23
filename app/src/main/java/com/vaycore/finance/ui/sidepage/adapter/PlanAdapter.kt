package com.vaycore.finance.ui.sidepage.adapter

import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseAdapter
import com.vaycore.finance.data.local.sideBean.PlanItem
import com.vaycore.finance.databinding.ItemSidepagePlanBinding
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.formatAmountWithPrefix
import java.math.BigDecimal
import java.math.RoundingMode

class PlanAdapter :
    BaseAdapter<PlanItem, ItemSidepagePlanBinding>(ItemSidepagePlanBinding::inflate) {

    var onSaveMoney: ((PlanItem) -> Unit)? = null

    override fun bindItem(
        binding: ItemSidepagePlanBinding,
        item: PlanItem,
        position: Int,
    ) = with(binding) {
        tvPlanName.text = item.planName.orEmpty()
        tvSavedAmount.text = item.savedAmount.formatAmountWithPrefix()
        tvGoalAmount.text = item.targetAmount.formatAmountWithPrefix()
        tvNextSavingTime.text = item.nextSaveDateText.orEmpty()
        tvNextSavingTime.isVisible = !item.nextSaveDateText.isNullOrBlank()
        tvNextSavingLabel.isVisible = tvNextSavingTime.isVisible

        tvDaysLeft.isVisible = item.remainingDays != null
        tvDaysLeft.text = item.remainingDays?.let {
            context.getString(R.string.portal_days_left, it)
        }.orEmpty()

        progressPlan.progress = item.progressValue()

        if (item.planIcon.isNullOrBlank()) {
            ivPlanIcon.setImageResource(R.mipmap.ic_product_defalut_img)
            ivPlanIcon.imageTintList = null
        } else {
            ivPlanIcon.imageTintList = null
            ivPlanIcon.loadImage(item.planIcon)
        }

        btSaveMoney.singleClick {
            onSaveMoney?.invoke(item)
        }
    }

    private fun PlanItem.progressValue(): Int {
        progressPercent?.let {
            return it.multiply(PROGRESS_SCALE)
                .setScale(0, RoundingMode.DOWN)
                .toInt()
                .coerceIn(0, PROGRESS_MAX)
        }
        val goal = targetAmount ?: return 0
        if (goal <= BigDecimal.ZERO) return 0
        return (savedAmount ?: BigDecimal.ZERO)
            .multiply(BigDecimal(100))
            .multiply(PROGRESS_SCALE)
            .divide(goal, 0, RoundingMode.DOWN)
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
    }

    companion object {
        private const val PROGRESS_MAX = 10000
        private val PROGRESS_SCALE = BigDecimal(100)
    }
}
