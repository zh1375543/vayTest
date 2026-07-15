package com.vaycore.finance.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.vaycore.finance.R

class AuthProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private val stepBar = LinearLayout(context)
    private val stepBadgeView = AppCompatTextView(context)
    private val stepLabelView = AppCompatTextView(context)
    private val stepPanel = FrameLayout(context)
    private val stepLineView = StepLineView(context)
    private val stepIconRow = LinearLayout(context)
    private val titleView = AppCompatTextView(context)
    private val descView = AppCompatTextView(context)

    init {
        orientation = VERTICAL
        setupStepBar()
        setupStepPanel()
        setupTitle()
    }

    fun bind(
        requiredTypes: List<String>,
        currentType: String,
        title: CharSequence,
        desc: CharSequence,
    ) {
        val steps = requiredTypes.mapNotNull { AuthStep.fromType(it) }
        val currentIndex = steps.indexOfFirst { it.type == currentType }
        val hasProgress = steps.isNotEmpty() && currentIndex >= 0
        visibility = if (hasProgress) VISIBLE else GONE
        if (!hasProgress) return

        titleView.text = title
        descView.text = desc
        val currentNumber = currentIndex + 1
        stepBadgeView.text = currentNumber.toString()
        stepLabelView.text = context.getString(R.string.auth_step_format, currentNumber, steps.size)
        renderStepIcons(steps, currentIndex)
    }

    private fun setupStepBar() {
        stepBar.orientation = HORIZONTAL
        stepBar.gravity = Gravity.CENTER_VERTICAL
        stepBar.setPadding(
            resources.getDimensionPixelSize(R.dimen.dp_16),
            0,
            resources.getDimensionPixelSize(R.dimen.dp_16),
            0,
        )
        stepBar.background = roundedDrawable(
            colorResId = R.color.color_7087F8,
            radius = resources.getDimension(R.dimen.dp_31),
        )
        addView(
            stepBar,
            LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.dp_40)).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.dp_20)
                marginEnd = resources.getDimensionPixelSize(R.dimen.dp_20)
            },
        )

        stepBadgeView.gravity = Gravity.CENTER
        stepBadgeView.includeFontPadding = false
        stepBadgeView.setTypeface(stepBadgeView.typeface, Typeface.BOLD)
        stepBadgeView.setTextColor(ContextCompat.getColor(context, R.color.C_111827))
        stepBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_12))
        stepBadgeView.background = roundedDrawable(
            colorResId = R.color.white,
            radius = resources.getDimension(R.dimen.dp_12),
        )
        stepBar.addView(
            stepBadgeView,
            LayoutParams(resources.getDimensionPixelSize(R.dimen.dp_18), resources.getDimensionPixelSize(
                R.dimen.dp_18)),
        )

        stepLabelView.gravity = Gravity.CENTER_VERTICAL
        stepLabelView.includeFontPadding = false
        stepLabelView.setTypeface(stepLabelView.typeface, Typeface.BOLD)
        stepLabelView.setTextColor(ContextCompat.getColor(context, R.color.white))
        stepLabelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_14))
        stepBar.addView(
            stepLabelView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.dp_12)
            },
        )
    }

    private fun setupTitle() {
        titleView.includeFontPadding = false
        titleView.setTypeface(titleView.typeface, Typeface.BOLD)
        titleView.setTextColor(ContextCompat.getColor(context, R.color.C_111827))
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_24))
        titleView.maxLines = 1
        titleView.ellipsize = TextUtils.TruncateAt.END
        addView(
            titleView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.dp_20)
                marginEnd = resources.getDimensionPixelSize(R.dimen.dp_20)
                topMargin = resources.getDimensionPixelSize(R.dimen.dp_18)
            },
        )

        descView.includeFontPadding = false
        descView.setTextColor(ContextCompat.getColor(context, R.color.C_6D7280))
        descView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_12))
        addView(
            descView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.dp_20)
                marginEnd = resources.getDimensionPixelSize(R.dimen.dp_20)
                topMargin = resources.getDimensionPixelSize(R.dimen.dp_6)
            },
        )
    }

    private fun setupStepPanel() {
        stepPanel.background = roundedStrokeDrawable(
            fillColorResId = R.color.white,
            strokeColorResId = R.color.color_7087F8,
            radius = resources.getDimension(R.dimen.dp_12),
            strokeWidth = resources.getDimensionPixelSize(R.dimen.dp_1),
        )
        addView(
            stepPanel,
            LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.dp_70)).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.dp_20)
                marginEnd = resources.getDimensionPixelSize(R.dimen.dp_20)
                topMargin = resources.getDimensionPixelSize(R.dimen.dp_8)
            },
        )

        stepPanel.addView(
            stepLineView,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.dp_1)).apply {
                gravity = Gravity.CENTER_VERTICAL
            },
        )

        stepIconRow.orientation = HORIZONTAL
        stepIconRow.gravity = Gravity.CENTER
        stepPanel.addView(
            stepIconRow,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    private fun renderStepIcons(steps: List<AuthStep>, currentIndex: Int) {
        stepIconRow.removeAllViews()
        val sidePadding = resources.getDimensionPixelSize(R.dimen.dp_10)
        stepIconRow.setPadding(sidePadding, 0, sidePadding, 0)
        val firstMarkerSize = markerSizeFor(index = 0, currentIndex = currentIndex)
        val lastMarkerSize = markerSizeFor(index = steps.lastIndex, currentIndex = currentIndex)
        stepLineView.setStepMetrics(
            count = steps.size,
            sidePadding = sidePadding,
            firstMarkerSize = firstMarkerSize,
            lastMarkerSize = lastMarkerSize,
        )
        steps.forEachIndexed { index, step ->
            val iconSize = markerSizeFor(index, currentIndex)
            val iconView = AppCompatImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setImageResource(iconResFor(step, index, currentIndex))
            }
            val holder = FrameLayout(context)
            holder.addView(
                iconView,
                FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                },
            )
            stepIconRow.addView(
                holder,
                LayoutParams(iconSize, LayoutParams.MATCH_PARENT),
            )
            if (index < steps.lastIndex) {
                stepIconRow.addView(
                    View(context),
                    LayoutParams(0, LayoutParams.MATCH_PARENT, 1f),
                )
            }
        }
    }

    private fun markerSizeFor(index: Int, currentIndex: Int): Int {
        return if (index == currentIndex) {
            resources.getDimensionPixelSize(R.dimen.dp_46)
        } else {
            resources.getDimensionPixelSize(R.dimen.dp_20)
        }
    }

    private fun iconResFor(step: AuthStep, index: Int, currentIndex: Int): Int {
        return when {
            index < currentIndex -> R.mipmap.ic_step_select
            index > currentIndex -> R.mipmap.ic_step_normal
            step == AuthStep.Bank -> R.mipmap.ic_step_bank
            step == AuthStep.Kyc -> R.mipmap.ic_step_kyc
            step == AuthStep.Personal -> R.mipmap.ic_step_personal_info
            step == AuthStep.Telecom -> R.mipmap.ic_step_service
            else -> R.mipmap.ic_step_normal
        }
    }

    private fun roundedDrawable(colorResId: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, colorResId))
        }
    }

    private fun roundedStrokeDrawable(
        fillColorResId: Int,
        strokeColorResId: Int,
        radius: Float,
        strokeWidth: Int,
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(context, fillColorResId))
            setStroke(strokeWidth, ContextCompat.getColor(context, strokeColorResId))
        }
    }

    private enum class AuthStep(val type: String) {
        Kyc("KYC"),
        Personal("ID"),
        Bank("BANK"),
        Telecom("TELECOM");

        companion object {
            fun fromType(type: String?): AuthStep? {
                return values().firstOrNull { it.type == type?.uppercase() }
            }
        }
    }

    private class StepLineView(context: Context) : View(context) {
        private var stepCount = 0
        private var sidePadding = 0
        private var firstMarkerSize = 0
        private var lastMarkerSize = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.C_D2D5DA)
            strokeWidth = context.resources.getDimension(R.dimen.dp_1)
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        fun setStepMetrics(
            count: Int,
            sidePadding: Int,
            firstMarkerSize: Int,
            lastMarkerSize: Int,
        ) {
            stepCount = count
            this.sidePadding = sidePadding
            this.firstMarkerSize = firstMarkerSize
            this.lastMarkerSize = lastMarkerSize
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (stepCount < 2) return
            val centerY = height / 2f
            val startX = sidePadding + firstMarkerSize / 2f
            val endX = width.toFloat() - sidePadding - lastMarkerSize / 2f
            if (endX <= startX) return
            canvas.drawLine(startX, centerY, endX, centerY, paint)
        }
    }
}