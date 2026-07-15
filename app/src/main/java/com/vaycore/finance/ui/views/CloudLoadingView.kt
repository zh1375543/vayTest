package com.vaycore.finance.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import com.vaycore.finance.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CloudLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // default values
    private val dBarColor = 0xFFCAECB0.toInt()
    private val dBarCount = 5
    private val dBarWidth = dp(3f)
    private val dBarSpacing = dp(4f)
    private val dRadius = dp(1.5f).toFloat()
    private val dMinHeightRatio = 0.25f
    private val dMaxHeightRatio = 0.95f
    private val dDuration = 900

    // properties
    @ColorInt private var barColor = dBarColor
    private var barCount = dBarCount
    private var barWidth = dBarWidth
    private var barSpacing = dBarSpacing
    private var radius: Float = dRadius.toFloat()
    private var minHeightRatio = dMinHeightRatio
    private var maxHeightRatio = dMaxHeightRatio
    private var duration = dDuration

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = barColor
    }
    private val rect = RectF()

    // animation: t from 0..2π, sine wave + phase offset
    private var anim: ValueAnimator? = null
    private var phase = 0f // 0..2π

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CloudLoadingView)
            barColor = a.getColor(R.styleable.CloudLoadingView_cl_barColor, dBarColor)
            barCount = a.getInt(R.styleable.CloudLoadingView_cl_barCount, dBarCount).coerceAtLeast(1)
            barWidth = a.getDimensionPixelSize(R.styleable.CloudLoadingView_cl_barWidth, dBarWidth)
            barSpacing = a.getDimensionPixelSize(R.styleable.CloudLoadingView_cl_barSpacing, dBarSpacing)
            radius = a.getDimension(R.styleable.CloudLoadingView_cl_radius, dRadius)
            minHeightRatio = a.getFloat(R.styleable.CloudLoadingView_cl_minHeightRatio, dMinHeightRatio)
            maxHeightRatio = a.getFloat(R.styleable.CloudLoadingView_cl_maxHeightRatio, dMaxHeightRatio)
            duration = a.getInt(R.styleable.CloudLoadingView_cl_duration, dDuration)
            a.recycle()
        }
        paint.color = barColor
        // validate min/max ratio
        if (minHeightRatio > maxHeightRatio) {
            val tmp = minHeightRatio
            minHeightRatio = maxHeightRatio
            maxHeightRatio = tmp
        }
        minHeightRatio = min(1f, max(0f, minHeightRatio))
        maxHeightRatio = min(1f, max(0f, maxHeightRatio))
        isClickable = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // reasonable min size: width = all bars + spacing, height = 24dp
        val desiredW = paddingLeft + paddingRight +
                barCount * barWidth + (barCount - 1) * barSpacing
        val desiredH = paddingTop + paddingBottom + dp(24f)

        val w = resolveSize(desiredW, widthMeasureSpec)
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val contentW = width - paddingLeft - paddingRight
        val contentH = height - paddingTop - paddingBottom

        // phase offset per bar (stagger the bounce)
        val perPhase = (2f * Math.PI / max(1, barCount)).toFloat()

        // actual height range
        val minH = contentH * minHeightRatio
        val maxH = contentH * maxHeightRatio
        val half = (minH + maxH) / 2f
        val amp = (maxH - minH) / 2f // amplitude

        // start X (center-aligned)
        val totalBarsWidth = barCount * barWidth + (barCount - 1) * barSpacing
        var x = paddingLeft + (contentW - totalBarsWidth) / 2f

        for (i in 0 until barCount) {
            val phi = phase + i * perPhase
            // sine value range -1..1, mapped to minH..maxH
            val h = (half + amp * sin(phi)).toFloat().coerceIn(minH, maxH)

            val barTop = paddingTop + (contentH - h) / 2f
            val barBottom = barTop + h

            rect.set(x, barTop, x + barWidth, barBottom)
            canvas.drawRoundRect(rect, radius, radius, paint)

            x += barWidth + barSpacing
        }
    }

    // --- Animation control ---
    private fun ensureAnim() {
        if (anim != null) return
        anim = ValueAnimator.ofFloat(0f, (2f * PI).toFloat()).apply {
            duration = this@CloudLoadingView.duration.toLong()
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
        }
    }

    fun start() {
        ensureAnim()
        if (anim?.isStarted != true) anim?.start()
    }

    fun stop() {
        anim?.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    // --- Public API ---
    fun setBarColor(@ColorInt color: Int) {
        barColor = color
        paint.color = color
        invalidate()
    }

    fun setBarCount(count: Int) {
        barCount = count.coerceAtLeast(1)
        requestLayout()
        invalidate()
    }

    fun setRange(minRatio: Float, maxRatio: Float) {
        minHeightRatio = min(1f, max(0f, minRatio))
        maxHeightRatio = min(1f, max(0f, maxRatio))
        if (minHeightRatio > maxHeightRatio) {
            val t = minHeightRatio; minHeightRatio = maxHeightRatio; maxHeightRatio = t
        }
        invalidate()
    }

    fun setSpeed(durationMs: Int) {
        duration = durationMs
        anim?.duration = duration.toLong()
    }

    private fun dp(v: Float): Int =
        (v * resources.displayMetrics.density + 0.5f).toInt()
}
