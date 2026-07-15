package com.vaycore.finance.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.vaycore.finance.R

/**
 * A self-styling button component that encapsulates filled / outline variants
 * with built-in pressed and disabled states.
 *
 * Supported XML attributes (declare-styleable [R.styleable.ActionButtonView]):
 *
 * | Attribute                  | Format      | Default                                      |
 * |----------------------------|-------------|----------------------------------------------|
 * | actionButtonVariant        | enum        | filled (0)                                   |
 * | buttonRadius               | dimension   | 50dp                                         |
 * | buttonSolidColor           | color\|ref  | filled → #7087F8, outline → transparent      |
 * | buttonPressedColor         | color\|ref  | auto-lightened 18% from solidColor           |
 * | buttonDisabledColor        | color\|ref  | normal fill color                            |
 * | buttonStrokeWidth          | dimension   | 1dp                                          |
 * | buttonStrokeColor          | color\|ref  | filled → transparent, outline → #7087F8      |
 * | buttonTextColor            | color\|ref  | filled → white, outline → #7087F8            |
 * | buttonDisabledTextColor    | color\|ref  | normal text color                            |
 * | buttonDisabledAlpha        | float       | 0.6                                          |
 */
class ActionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    /** Visual variant: [VARIANT_FILLED] (solid bg) or [VARIANT_OUTLINE] (transparent bg + stroke). */
    private var variant = VARIANT_FILLED
    /** Corner radius of the background drawable. */
    private var radius = resources.getDimension(R.dimen.dp_50)
    /** Normal-state fill color. null → use variant default. */
    private var solidColor: Int? = null
    /** Pressed-state fill color. null → auto-lightened from solidColor. */
    private var pressedColor: Int? = null
    /** Disabled-state fill color. null → keep the normal fill color. */
    private var disabledColor: Int? = null
    /** Stroke (border) width in px. */
    private var strokeWidth = resources.getDimensionPixelSize(R.dimen.dp_1)
    /** Normal-state stroke color. null → use variant default. */
    private var strokeColor: Int? = null
    /** Normal-state text color. null → use variant default. */
    private var normalTextColor: Int? = null
    /** Disabled-state text color. null → keep the normal text color. */
    private var disabledTextColor: Int? = null
    /** Opacity applied to the whole button while disabled. */
    private var disabledAlpha = DEFAULT_DISABLED_ALPHA

    init {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        minHeight = resources.getDimensionPixelSize(R.dimen.dp_44)
        includeFontPadding = false
        typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.sp_16))
        parseAttributes(attrs, defStyleAttr)
        render()
        updateEnabledAlpha()
    }

    /** Switch between [VARIANT_FILLED] and [VARIANT_OUTLINE] at runtime. */
    fun setVariant(value: Int) {
        if (variant == value) return
        variant = value
        render()
    }

    fun applyStyle(
        variant: Int,
        solidColor: Int? = null,
        strokeColor: Int? = null,
        textColor: Int? = null,
    ) {
        this.variant = variant
        this.solidColor = solidColor
        this.strokeColor = strokeColor
        this.normalTextColor = textColor
        render()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateEnabledAlpha()
    }

    private fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.ActionButtonView,
            defStyleAttr,
            0,
        )
        variant = typedArray.getInt(R.styleable.ActionButtonView_actionButtonVariant, VARIANT_FILLED)
        radius = typedArray.getDimension(R.styleable.ActionButtonView_buttonRadius, radius)
        solidColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonSolidColor)
        pressedColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonPressedColor)
        disabledColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonDisabledColor)
        strokeWidth = typedArray.getDimensionPixelSize(
            R.styleable.ActionButtonView_buttonStrokeWidth,
            strokeWidth,
        )
        strokeColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonStrokeColor)
        normalTextColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonTextColor)
        disabledTextColor = typedArray.getColorOrNull(R.styleable.ActionButtonView_buttonDisabledTextColor)
        disabledAlpha = typedArray.getFloat(
            R.styleable.ActionButtonView_buttonDisabledAlpha,
            DEFAULT_DISABLED_ALPHA,
        ).coerceIn(0f, ENABLED_ALPHA)
        typedArray.recycle()
    }

    private fun updateEnabledAlpha() {
        alpha = if (isEnabled) ENABLED_ALPHA else disabledAlpha
    }

    private fun render() {
        val themeColor = ContextCompat.getColor(context, R.color.color_7087F8)
        val filledText = ContextCompat.getColor(context, R.color.white)

        val defaultSolid = if (variant == VARIANT_OUTLINE) Color.TRANSPARENT else themeColor
        val defaultStroke = if (variant == VARIANT_OUTLINE) themeColor else Color.TRANSPARENT
        val defaultText = if (variant == VARIANT_OUTLINE) themeColor else filledText
        val resolvedSolid = solidColor ?: defaultSolid
        val resolvedStroke = strokeColor ?: defaultStroke
        val resolvedText = normalTextColor ?: defaultText

        background = createStateDrawable(
            normalFill = resolvedSolid,
            pressedFill = pressedColor ?: lightenColor(resolvedSolid),
            disabledFill = disabledColor ?: resolvedSolid,
            normalStroke = resolvedStroke,
            disabledStroke = resolvedStroke,
        )
        setTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf(),
                ),
                intArrayOf(
                    disabledTextColor ?: resolvedText,
                    resolvedText,
                ),
            ),
        )
    }

    private fun createStateDrawable(
        normalFill: Int,
        pressedFill: Int,
        disabledFill: Int,
        normalStroke: Int,
        disabledStroke: Int,
    ): StateListDrawable {
        return StateListDrawable().apply {
            addState(
                intArrayOf(-android.R.attr.state_enabled),
                createDrawable(disabledFill, disabledStroke),
            )
            addState(
                intArrayOf(android.R.attr.state_pressed),
                createDrawable(pressedFill, normalStroke),
            )
            addState(
                intArrayOf(),
                createDrawable(normalFill, normalStroke),
            )
        }
    }

    private fun createDrawable(fillColor: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            if (strokeColor != Color.TRANSPARENT && strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun TypedArray.getColorOrNull(index: Int): Int? {
        return if (hasValue(index)) getColor(index, Color.TRANSPARENT) else null
    }

    private fun lightenColor(color: Int): Int {
        if (color == Color.TRANSPARENT) return Color.TRANSPARENT
        return Color.argb(
            Color.alpha(color),
            lightenChannel(Color.red(color)),
            lightenChannel(Color.green(color)),
            lightenChannel(Color.blue(color)),
        )
    }

    private fun lightenChannel(value: Int): Int {
        return (value + (255 - value) * PRESSED_LIGHTEN_FRACTION)
            .toInt()
            .coerceIn(0, 255)
    }

    companion object {
        /** Solid fill with no visible stroke by default. */
        const val VARIANT_FILLED = 0
        /** Transparent fill with a colored stroke border. */
        const val VARIANT_OUTLINE = 1
        /** How much to lighten each RGB channel for the pressed state (0–1). */
        private const val PRESSED_LIGHTEN_FRACTION = 0.18f
        private const val ENABLED_ALPHA = 1f
        private const val DEFAULT_DISABLED_ALPHA = 0.6f
    }
}
