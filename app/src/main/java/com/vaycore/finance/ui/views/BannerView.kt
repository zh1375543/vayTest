package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.vaycore.finance.R
import com.vaycore.finance.base.createAdapter
import com.vaycore.finance.databinding.BannerAdapterBinding
import com.vaycore.finance.data.local.bean.CampaignBannerResponse
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.ui.extension.setRoundedRectangleBackground
import com.vaycore.finance.ui.activities.WebViewActivity
import com.vaycore.finance.ui.extension.loadImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    // ViewPager2
    private val viewPager2: ViewPager2 = ViewPager2(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        offscreenPageLimit = 2
    }
    private val indicatorLayout: LinearLayout
    private var autoScrollJob: Job? = null
    private val adapter by lazy {
        createAdapter<CampaignBannerResponse, BannerAdapterBinding>(BannerAdapterBinding::inflate) { item, _ ->
            ivIcon.loadImage(item.activityPicUrl)
        }.apply {
            setOnItemClickListener { item, _ ->
                if (!item.activityH5Url.isNullOrBlank()) {
                    WebViewActivity.launch(context, item.name ?: "", item.activityH5Url)
                }
            }
        }
    }

    private var data: List<CampaignBannerResponse> = emptyList()
    private val indicators = mutableListOf<View>()

    private val autoScrollDelay = 5000L // 5 seconds

    init {
        addView(viewPager2)

        // bottom indicator container
        indicatorLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 4.dp
            }
        }
        addView(indicatorLayout)

        // infinite loop: listen for page changes
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val realCount = data.size
                if (realCount == 0) return

                when (position) {
                    0 -> { // hit fake tail (loopList[0] = last image)
                        // jump to real last without animation
                        viewPager2.setCurrentItem(realCount, false)
                        updateIndicator(realCount - 1)
                    }

                    realCount + 1 -> { // hit fake head (loopList[last] = first image)
                        // jump to real first without animation
                        viewPager2.setCurrentItem(1, false)
                        updateIndicator(0)
                    }

                    else -> {
                        // normal position -1 maps to real index
                        updateIndicator(position - 1)
                    }
                }
            }
        })
    }

    fun setData(list: List<CampaignBannerResponse>) {
        data = list
        if (list.isEmpty()) return

        // build virtual list (add fake head/tail)
        val loopList = mutableListOf<CampaignBannerResponse>()
        loopList.add(list.last())
        loopList.addAll(list)
        loopList.add(list.first())

        adapter.submitItems(loopList)
        viewPager2.adapter = adapter
        viewPager2.setCurrentItem(1, false) // show first image by default

        setupIndicators(list.size)
        updateIndicator(0)
        startAutoScroll()
    }

    private fun setupIndicators(count: Int) {
        indicatorLayout.removeAllViews()
        indicators.clear()

        repeat(count) {
            val dot = View(context).apply {
                setRoundedRectangleBackground(
                    context.getColor2(R.color.C_9CA3AF),
                    context.resources.getDimension(R.dimen.dp_11)
                )
                val margin = 2.dp
                layoutParams = LinearLayout.LayoutParams(6.dp, 6.dp).apply {
                    leftMargin = margin
                    rightMargin = margin
                }
            }
            indicatorLayout.addView(dot)
            indicators.add(dot)
        }
    }

    private fun updateIndicator(index: Int) {
        indicators.forEachIndexed { i, dot ->
            dot.setRoundedRectangleBackground(
                context.getColor2(if (i == index) R.color.color_7087F8 else R.color.C_9CA3AF),
                context.resources.getDimension(R.dimen.dp_11)
            )
        }
    }

    private fun startAutoScroll() {
        stopAutoScroll()
        autoScrollJob = (context as? LifecycleOwner)?.lifecycleScope?.launch {
            while (isActive) {
                delay(autoScrollDelay.milliseconds)
                val next = viewPager2.currentItem + 1
                val realCount = data.size

                when (next) {
                    0 -> {
                        // should not happen, defensive code
                        viewPager2.setCurrentItem(realCount, false)
                    }

                    realCount + 1 -> {
                        // hit fake head, jump to real first page without animation
                        viewPager2.setCurrentItem(1, false)
                    }

                    else -> {
                        // normal page change (with animation)
                        viewPager2.setCurrentItem(next, true)
                    }
                }
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoScroll()
    }

    // extension: dp to px
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
