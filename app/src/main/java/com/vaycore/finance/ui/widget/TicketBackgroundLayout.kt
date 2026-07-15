package com.vaycore.finance.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.vaycore.finance.R
import kotlin.math.max

class TicketBackgroundLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val cardRect = RectF()
    private val anchorRect = Rect()
    private val cardPath = Path()
    private val cutoutPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var cornerRadius = resources.getDimension(R.dimen.dp_14)
    private var sideCutoutRadius = resources.getDimension(R.dimen.dp_18)
    private var sideCutoutCenterPercent = DEFAULT_SIDE_CUTOUT_CENTER_PERCENT
    private var sideCutoutCenterFromBottom = NO_DIMENSION
    private var sideCutoutCenterFromBottomOverride = NO_DIMENSION
    private var sideCutoutAnchorId = View.NO_ID
    private var sideCutoutAnchorVerticalBias = DEFAULT_ANCHOR_VERTICAL_BIAS
    private var sideCutoutAnchorOffsetY = 0f
    private var sideCutoutVisible = true

    init {
        setWillNotDraw(false)
        parseAttributes(attrs, defStyleAttr)
    }

    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) {
            super.onDraw(canvas)
            return
        }

        cardRect.set(0f, 0f, width.toFloat(), height.toFloat())
        cardPath.reset()
        cardPath.addRoundRect(
            cardRect,
            max(0f, cornerRadius),
            max(0f, cornerRadius),
            Path.Direction.CW,
        )

        cutoutPath.reset()
        addSideCutouts()
        cardPath.op(cutoutPath, Path.Op.DIFFERENCE)

        canvas.drawPath(cardPath, paint)
        super.onDraw(canvas)
    }

    private fun addSideCutouts() {
        val radius = max(0f, sideCutoutRadius)
        if (!sideCutoutVisible || radius == 0f) return

        val centerY = resolveSideCutoutCenterY()
        cutoutPath.addCircle(0f, centerY, radius, Path.Direction.CW)
        cutoutPath.addCircle(width.toFloat(), centerY, radius, Path.Direction.CW)
    }

    fun setSideCutoutVisible(visible: Boolean) {
        if (sideCutoutVisible == visible) return

        sideCutoutVisible = visible
        invalidate()
    }

    fun setSideCutoutBottomClearance(bottomClearance: Float?) {
        val newValue = bottomClearance?.let { it.coerceAtLeast(0f) + max(0f, sideCutoutRadius) }
            ?: NO_DIMENSION
        if (sideCutoutCenterFromBottomOverride == newValue) return

        sideCutoutCenterFromBottomOverride = newValue
        invalidate()
    }

    private fun resolveSideCutoutCenterY(): Float {
        if (sideCutoutCenterFromBottomOverride >= 0f) {
            return height - sideCutoutCenterFromBottomOverride
        }

        val anchor = if (sideCutoutAnchorId != View.NO_ID) findViewById<View>(sideCutoutAnchorId) else null
        if (anchor != null && anchor.height > 0) {
            anchor.getDrawingRect(anchorRect)
            offsetDescendantRectToMyCoords(anchor, anchorRect)
            val bias = sideCutoutAnchorVerticalBias.coerceIn(0f, 1f)
            return anchorRect.top + anchorRect.height() * bias + sideCutoutAnchorOffsetY
        }
        if (sideCutoutCenterFromBottom >= 0f) {
            return height - sideCutoutCenterFromBottom
        }
        return height * sideCutoutCenterPercent.coerceIn(0f, 1f)
    }

    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.TicketBackgroundLayout,
            defStyleAttr,
            0,
        )
        paint.color = ta.getColor(
            R.styleable.TicketBackgroundLayout_ticketColor,
            ContextCompat.getColor(context, R.color.color_7087F8),
        )
        cornerRadius = ta.getDimension(
            R.styleable.TicketBackgroundLayout_ticketCornerRadius,
            cornerRadius,
        )
        sideCutoutRadius = ta.getDimension(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutRadius,
            sideCutoutRadius,
        )
        sideCutoutCenterPercent = ta.getFloat(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutCenterPercent,
            sideCutoutCenterPercent,
        )
        sideCutoutCenterFromBottom = ta.getDimension(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutCenterFromBottom,
            sideCutoutCenterFromBottom,
        )
        sideCutoutAnchorId = ta.getResourceId(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutAnchor,
            View.NO_ID,
        )
        sideCutoutAnchorVerticalBias = ta.getFloat(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutAnchorVerticalBias,
            sideCutoutAnchorVerticalBias,
        )
        sideCutoutAnchorOffsetY = ta.getDimension(
            R.styleable.TicketBackgroundLayout_ticketSideCutoutAnchorOffsetY,
            sideCutoutAnchorOffsetY,
        )
        ta.recycle()
    }

    private companion object {
        const val DEFAULT_SIDE_CUTOUT_CENTER_PERCENT = 0.82f
        const val DEFAULT_ANCHOR_VERTICAL_BIAS = 0.5f
        const val NO_DIMENSION = -1f
    }
}
