package com.vaycore.finance.ui.widget.shape

import android.graphics.Color

/**
 * Background configuration parsed from ShapeView XML attributes.
 *
 * A zero state color or gradient value means that the value is not configured.
 */
/**
 * Complete background definition for one Shape View.
 *
 * Each state is kept separately so the drawable factory can preserve Android's
 * state-list precedence without requiring a View-specific implementation.
 */
internal data class ShapeAppearance(
    val corners: ShapeCorners,
    val strokeWidth: Int,
    val gradientOrientation: ShapeGradientOrientation,
    val autoFillPressedColor: Boolean,
    val normal: ShapeStateAppearance,
    val pressed: ShapeStateAppearance,
    val focused: ShapeStateAppearance,
    val selected: ShapeStateAppearance,
    val disabled: ShapeStateAppearance,
)

/** Pixel corner radii after the XML attributes have been resolved. */
internal data class ShapeCorners(
    val topLeft: Int,
    val topRight: Int,
    val bottomRight: Int,
    val bottomLeft: Int,
)

/** Fill, stroke, and optional gradient values for a single View state. */
internal data class ShapeStateAppearance(
    val fillColor: Int,
    val strokeColor: Int,
    val gradient: ShapeGradient,
)

/**
 * A linear gradient definition. A gradient is active only when both endpoint
 * colors are configured.
 */
internal data class ShapeGradient(
    val startColor: Int = Color.TRANSPARENT,
    val centerColor: Int = Color.TRANSPARENT,
    val endColor: Int = Color.TRANSPARENT,
) {
    val isEnabled: Boolean
        get() = startColor != Color.TRANSPARENT && endColor != Color.TRANSPARENT
}

/**
 * Represents the XML gradient direction values used by the drawable factory.
 */
internal enum class ShapeGradientOrientation(
    val attributeValue: Int,
) {
    LEFT_RIGHT(0),
    RIGHT_LEFT(1),
    TOP_BOTTOM(2),
    BOTTOM_TOP(3),
    TOP_LEFT_BOTTOM_RIGHT(4),
    TOP_RIGHT_BOTTOM_LEFT(5),
    BOTTOM_LEFT_TOP_RIGHT(6),
    BOTTOM_RIGHT_TOP_LEFT(7),
    ;

    companion object {
        fun fromAttributeValue(value: Int): ShapeGradientOrientation {
            return entries.firstOrNull { it.attributeValue == value } ?: LEFT_RIGHT
        }
    }
}
