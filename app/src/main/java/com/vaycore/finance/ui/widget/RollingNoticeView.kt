package com.vaycore.finance.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.vaycore.finance.R
import com.vaycore.finance.util.context.getColor2
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Vertical seamless notice marquee: content slides from top to bottom.
 * Duplicates the same data twice for a seamless loop; the viewport height is fixed to a single line.
 */
class RollingNoticeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {

    private val container = LinearLayout(context)
    private var animator: ValueAnimator? = null

    // Text size of a single line
    private val textSizeSp = 11f

    // Pixels scrolled per second (smaller = slower)
    private val speed = 45f

    // Single-line (viewport) height; onMeasure pins the whole view's height to this
    private val itemHeight: Int = measureItemHeight()

    init {
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        isFillViewport = true

        container.orientation = LinearLayout.VERTICAL
        container.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        addView(container)
    }

    // The viewport shows only one line; lock the height to a single line
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fixedHeightSpec = MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, fixedHeightSpec)
    }

    // ===============================
    // Set data
    // ===============================
    fun setTexts(texts: List<String> = generateMaskedNumbers(), isWhiteColor: Boolean = true) {
        stop()

        container.removeAllViews()
        if (texts.isEmpty()) return

        // Duplicate the data twice for a seamless loop
        (texts + texts).forEach { text ->
            container.addView(createTextView(text, isWhiteColor))
        }

        post {
            // Total height of the first copy, i.e. the displacement of one loop
            startScroll(itemHeight * texts.size)
        }
    }

    private fun createTextView(text: String, isWhiteColor: Boolean): TextView {
        return TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            maxLines = 1
            setTextColor(context.getColor2(if (isWhiteColor) R.color.C_111827 else R.color.C_111827))
            textSize = textSizeSp
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                itemHeight
            )
        }
    }

    // ===============================
    // Start scrolling (top to bottom)
    // ===============================
    private fun startScroll(blockHeight: Int) {
        if (blockHeight <= 0) return

        val duration = ((blockHeight / speed) * 1000).toLong()

        // Offset decreases from large to small -> content visually slides from top to bottom
        animator = ValueAnimator.ofInt(blockHeight, 0).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val value = it.animatedValue as Int
                scrollTo(0, value)
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    // Estimate the single-line height from the text size (with a little vertical padding)
    private fun measureItemHeight(): Int {
        val textSizePx = textSizeSp * resources.displayMetrics.scaledDensity
        val fm = Paint().apply { textSize = textSizePx }.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val verticalPadding = 4f * resources.displayMetrics.density
        return ceil(lineHeight + verticalPadding).toInt()
    }

    // ===============================
    // Generate mock data
    // ===============================
    private fun generateMaskedNumbers(count: Int = 50): List<String> {
        val prefixes = listOf(
            "086", "096", "097", "098", "032", "033", "034", "035",
            "036", "037", "038", "039",
            "089", "090", "093", "070", "079", "077", "076", "078",
            "088", "091", "094", "081", "082", "083", "084", "085",
            "092", "056", "058", "099", "059"
        )

        return List(count) {
            val prefix = prefixes.random()
            val suffix = Random.nextInt(10, 99)
            context.getString(
                R.string.users_notice,
                "$prefix*****$suffix"
            )
        }
    }
}
