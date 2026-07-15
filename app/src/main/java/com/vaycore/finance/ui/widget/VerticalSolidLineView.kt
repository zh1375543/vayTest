package com.vaycore.finance.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import androidx.core.content.ContextCompat
import com.vaycore.finance.R

class VerticalSolidLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }

    private var lineColor = ContextCompat.getColor(context, R.color.C_9CA3AF)
    private var lineStrokeWidth = resources.displayMetrics.density

    init {
        applyViewAttributes(attrs)
        refreshPaint()
    }

    private fun applyViewAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        context.obtainStyledAttributes(attrs, R.styleable.VerticalSolidLineView).use { typedArray ->
            lineColor = typedArray.getColor(
                R.styleable.VerticalSolidLineView_solidLineColor,
                lineColor
            )
            lineStrokeWidth = typedArray.getDimension(
                R.styleable.VerticalSolidLineView_solidLineWidth,
                lineStrokeWidth
            )
        }
    }

    private fun refreshPaint() {
        linePaint.color = lineColor
        linePaint.strokeWidth = lineStrokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCenteredLine(canvas)
    }

    private fun drawCenteredLine(canvas: Canvas) {
        val centerX = width / 2f
        canvas.drawLine(centerX, 0f, centerX, height.toFloat(), linePaint)
    }
}
