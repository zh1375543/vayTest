package com.vaycore.finance.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import com.vaycore.finance.R
import com.vaycore.finance.data.local.bean.SelectionOption
import kotlin.math.abs
import kotlin.math.roundToInt

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val itemHeight by lazy { dp(50) }
    private val visibleCount = 5

    private val centerColor by lazy { ContextCompat.getColor(context, R.color.C_111827) }
    private val normalColor by lazy { ContextCompat.getColor(context, R.color.C_9CA3AF) }

    private val centerPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(18f)
            textAlign = Paint.Align.CENTER
            color = centerColor
            isFakeBoldText = true
        }
    }
    private val normalPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(15f)
            textAlign = Paint.Align.CENTER
            color = normalColor
        }
    }

    private var dataList: List<SelectionOption> = emptyList()

    // single source of truth: currently selected index
    private var selectedIndex = 0

    // scroll offset in pixels, 0 = selectedIndex centered
    private var scrollOffset = 0f

    private val scroller = OverScroller(context)
    private var lastTouchY = 0f
    private var isDragging = false

    private var onSelectListener: ((position: Int, value: SelectionOption) -> Unit)? = null

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                scroller.fling(
                    0, scrollOffset.toInt(),
                    0, (-vY).toInt(),
                    0, 0,
                    -selectedIndex * itemHeight,
                    (dataList.size - 1 - selectedIndex) * itemHeight
                )
                postInvalidateOnAnimation()
                return true
            }
        })

    init {
        isHapticFeedbackEnabled = true
    }

    // =====================
    // Public API
    // =====================

    fun setData(list: List<SelectionOption>, targetIndex: Int = 0) {
        dataList = list
        selectedIndex = targetIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        scrollOffset = 0f
        scroller.forceFinished(true)
        invalidate()
        // dispatch callback immediately, no post needed
        dispatchSelect()
    }

    fun setDefaultSelected(index: Int) {
        if (dataList.isEmpty()) return
        selectedIndex = index.coerceIn(0, dataList.lastIndex)
        scrollOffset = 0f
        scroller.forceFinished(true)
        invalidate()
        dispatchSelect()
    }

    fun getSelectedPosition(): Int = selectedIndex

    fun getSelectedValue(): SelectionOption? = dataList.getOrNull(selectedIndex)

    fun setOnSelectListener(block: ((position: Int, value: SelectionOption) -> Unit)?) {
        onSelectListener = block
    }

    // =====================
    // Touch & Scroll
    // =====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                lastTouchY = event.y
                isDragging = true
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = lastTouchY - event.y
                lastTouchY = event.y
                scrollOffset += dy
                scrollOffset = scrollOffset.coerceIn(
                    -selectedIndex * itemHeight.toFloat(),
                    (dataList.size - 1 - selectedIndex) * itemHeight.toFloat()
                )
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                snapToItem()
            }
        }
        return true
    }

    private fun snapToItem() {
        // find the nearest item
        val delta = (scrollOffset / itemHeight).roundToInt()
        val targetIndex = (selectedIndex + delta).coerceIn(0, dataList.lastIndex)
        val targetOffset = delta * itemHeight.toFloat()

        scroller.startScroll(
            0, scrollOffset.toInt(),
            0, (targetOffset - scrollOffset).toInt(),
            200
        )
        postInvalidateOnAnimation()
    }

    override fun computeScroll() {
        if (dataList.isEmpty()) return  // guard against empty data

        if (scroller.computeScrollOffset()) {
            scrollOffset = scroller.currY.toFloat()
            scrollOffset = scrollOffset.coerceIn(
                -selectedIndex * itemHeight.toFloat(),
                (dataList.size - 1 - selectedIndex) * itemHeight.toFloat()
            )
            postInvalidateOnAnimation()
        } else {
            if (!isDragging) {
                val delta = (scrollOffset / itemHeight).roundToInt()
                val newIndex = (selectedIndex + delta).coerceIn(0, dataList.lastIndex)
                if (newIndex != selectedIndex || scrollOffset != 0f) {
                    selectedIndex = newIndex
                    scrollOffset = 0f
                    dispatchSelect()
                    invalidate()
                }
            }
        }
    }

    // =====================
    // Drawing
    // =====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataList.isEmpty()) return

        val centerX = width / 2f
        val centerY = height / 2f

        for (i in dataList.indices) {
            val relativeIndex = i - selectedIndex
            val itemCenterY = centerY + relativeIndex * itemHeight - scrollOffset

            // only draw visible items
            if (itemCenterY < -itemHeight || itemCenterY > height + itemHeight) continue

            val distance = abs(itemCenterY - centerY)
            val ratio = distance / (visibleCount / 2f * itemHeight)

            val paint = if (distance < itemHeight / 2f) centerPaint else normalPaint
            val alpha = ((1f - ratio * 0.6f) * 255).toInt().coerceIn(60, 255)
            paint.alpha = alpha

            // 3D tilt effect via canvas transforms
            val rotationX = ratio * 30f * if (itemCenterY < centerY) 1f else -1f
            val scale = 1f - ratio * 0.2f

            canvas.save()
            canvas.translate(centerX, itemCenterY)
            canvas.scale(scale, scale)

            // simulate rotationX with canvas
            val textY = -((paint.descent() + paint.ascent()) / 2)
            canvas.drawText(dataList[i].info, 0f, textY, paint)
            canvas.restore()
        }
    }

    // =====================
    // Measurement
    // =====================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = itemHeight * visibleCount
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    // =====================
    // Utilities
    // =====================

    private fun dispatchSelect() {
        val value = dataList.getOrNull(selectedIndex) ?: return
        onSelectListener?.invoke(selectedIndex, value)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}