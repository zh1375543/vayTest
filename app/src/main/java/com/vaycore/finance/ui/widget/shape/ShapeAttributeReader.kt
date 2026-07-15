package com.vaycore.finance.ui.widget.shape

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.vaycore.finance.R

/** Reads ShapeView XML attributes without applying them to a View. */
internal class ShapeAttributeReader(
    private val context: Context,
) {

    fun read(attrs: AttributeSet?): ShapeAttributes {
        return withShapeAttributes(attrs) { attributes ->
            ShapeAttributes(
                appearance = readShapeAppearance(attributes),
                textState = readTextStateAppearance(attributes),
            )
        }
    }

    /** Reads only background attributes for non-text Shape Views. */
    fun readAppearance(attrs: AttributeSet?): ShapeAppearance {
        return withShapeAttributes(attrs, ::readShapeAppearance)
    }

    private fun readShapeAppearance(
        attributes: android.content.res.TypedArray,
    ): ShapeAppearance {
        val radius = attributes.getDimensionPixelOffset(R.styleable.ShapeView_radius, DEFAULT_RADIUS)
        // An unset per-corner value inherits the shared radius.
        val topLeftRadius = attributes.getDimensionPixelOffset(
            R.styleable.ShapeView_left_top_radius,
            DEFAULT_RADIUS,
        ).orDefault(radius)
        val topRightRadius = attributes.getDimensionPixelOffset(
            R.styleable.ShapeView_right_top_radius,
            DEFAULT_RADIUS,
        ).orDefault(radius)
        val bottomRightRadius = attributes.getDimensionPixelOffset(
            R.styleable.ShapeView_right_bottom_radius,
            DEFAULT_RADIUS,
        ).orDefault(radius)
        val bottomLeftRadius = attributes.getDimensionPixelOffset(
            R.styleable.ShapeView_left_bottom_radius,
            DEFAULT_RADIUS,
        ).orDefault(radius)

        val orientation = ShapeGradientOrientation.fromAttributeValue(
            attributes.getInt(R.styleable.ShapeView_gradientOrientation, 0),
        )
        // The focused state reuses the normal fill and gradient; only its stroke can differ.
        val normalGradient = attributes.readGradient(
            startIndex = R.styleable.ShapeView_gradientStartColor,
            centerIndex = R.styleable.ShapeView_gradientCenterColor,
            endIndex = R.styleable.ShapeView_gradientEndColor,
        )

        val normalFill = attributes.getColor(
            R.styleable.ShapeView_solidColor,
            Color.TRANSPARENT,
        )
        val normalStroke = attributes.getColor(
            R.styleable.ShapeView_strokeColor,
            Color.TRANSPARENT,
        )

        return ShapeAppearance(
            corners = ShapeCorners(
                topLeft = topLeftRadius,
                topRight = topRightRadius,
                bottomRight = bottomRightRadius,
                bottomLeft = bottomLeftRadius,
            ),
            strokeWidth = attributes.getDimensionPixelOffset(
                R.styleable.ShapeView_strokeWidth,
                DEFAULT_STROKE_WIDTH,
            ),
            gradientOrientation = orientation,
            autoFillPressedColor = attributes.getBoolean(
                R.styleable.ShapeView_autoFillPressedColor,
                false,
            ),
            normal = ShapeStateAppearance(
                fillColor = normalFill,
                strokeColor = normalStroke,
                gradient = normalGradient,
            ),
            pressed = ShapeStateAppearance(
                fillColor = attributes.getColor(R.styleable.ShapeView_pressedColor, UNSET_COLOR),
                strokeColor = attributes.getColor(
                    R.styleable.ShapeView_pressedStrokeColor,
                    UNSET_COLOR,
                ),
                gradient = attributes.readGradient(
                    startIndex = R.styleable.ShapeView_pressedGradientStartColor,
                    endIndex = R.styleable.ShapeView_pressedGradientEndColor,
                ),
            ),
            focused = ShapeStateAppearance(
                fillColor = normalFill,
                strokeColor = attributes.getColor(
                    R.styleable.ShapeView_focusedStrokeColor,
                    UNSET_COLOR,
                ),
                gradient = normalGradient,
            ),
            selected = ShapeStateAppearance(
                fillColor = attributes.getColor(R.styleable.ShapeView_selectedColor, UNSET_COLOR),
                strokeColor = attributes.getColor(
                    R.styleable.ShapeView_selectedStrokeColor,
                    UNSET_COLOR,
                ),
                gradient = attributes.readGradient(
                    startIndex = R.styleable.ShapeView_selectedGradientStartColor,
                    endIndex = R.styleable.ShapeView_selectedGradientEndColor,
                ),
            ),
            disabled = ShapeStateAppearance(
                fillColor = attributes.getColor(R.styleable.ShapeView_disabledColor, UNSET_COLOR),
                strokeColor = attributes.getColor(
                    R.styleable.ShapeView_disabledStrokeColor,
                    UNSET_COLOR,
                ),
                gradient = attributes.readGradient(
                    startIndex = R.styleable.ShapeView_disabledGradientStartColor,
                    endIndex = R.styleable.ShapeView_disabledGradientEndColor,
                ),
            ),
        )
    }

    private fun readTextStateAppearance(
        attributes: android.content.res.TypedArray,
    ): TextStateAppearance {
        return TextStateAppearance(
            pressedTextColor = attributes.getColor(
                R.styleable.ShapeView_textColorPressed,
                UNSET_COLOR,
            ),
            selectedTextColor = attributes.getColor(
                R.styleable.ShapeView_textColorSelected,
                UNSET_COLOR,
            ),
            disabledTextColor = attributes.getColor(
                R.styleable.ShapeView_textColorDisabled,
                UNSET_COLOR,
            ),
            selectedDrawables = SelectedDrawables(
                startResId = attributes.getResourceId(
                    R.styleable.ShapeView_drawableStartSelected,
                    NO_RESOURCE,
                ),
                topResId = attributes.getResourceId(
                    R.styleable.ShapeView_drawableTopSelected,
                    NO_RESOURCE,
                ),
                endResId = attributes.getResourceId(
                    R.styleable.ShapeView_drawableEndSelected,
                    NO_RESOURCE,
                ),
                bottomResId = attributes.getResourceId(
                    R.styleable.ShapeView_drawableBottomSelected,
                    NO_RESOURCE,
                ),
            ),
            drawableSize = CompoundDrawableSize(
                width = attributes.getDimensionPixelSize(
                    R.styleable.ShapeView_drawableWidth,
                    NO_DIMENSION,
                ),
                height = attributes.getDimensionPixelSize(
                    R.styleable.ShapeView_drawableHeight,
                    NO_DIMENSION,
                ),
            ),
        )
    }

    private inline fun <T> withShapeAttributes(
        attrs: AttributeSet?,
        block: (android.content.res.TypedArray) -> T,
    ): T {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ShapeView)
        try {
            return block(attributes)
        } finally {
            attributes.recycle()
        }
    }

    private fun android.content.res.TypedArray.readGradient(
        startIndex: Int,
        endIndex: Int,
        centerIndex: Int? = null,
    ): ShapeGradient {
        // A zero center color produces a two-stop gradient instead of a three-stop gradient.
        return ShapeGradient(
            startColor = getColor(startIndex, UNSET_COLOR),
            centerColor = centerIndex?.let { index ->
                if (hasValue(index)) getColor(index, UNSET_COLOR) else UNSET_COLOR
            } ?: UNSET_COLOR,
            endColor = getColor(endIndex, UNSET_COLOR),
        )
    }

    private fun Int.orDefault(defaultValue: Int): Int {
        return if (this == DEFAULT_RADIUS) defaultValue else this
    }

    private companion object {
        const val DEFAULT_RADIUS = 0
        const val DEFAULT_STROKE_WIDTH = 0
        const val UNSET_COLOR = 0
        const val NO_RESOURCE = 0
        const val NO_DIMENSION = 0
    }
}
