package com.vaycore.finance.ui.widget.shape

import android.R as AndroidR
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Converts a parsed ShapeAppearance into Android drawables.
 *
 * This class contains no View references, so rendering stays independent from
 * the lifecycle of the View that receives the result.
 */
internal class ShapeDrawableFactory {

    fun create(
        appearance: ShapeAppearance,
        isSelectedAtCreation: Boolean,
    ): ShapeDrawableSet {
        val resolvedPressed = resolvePressedState(
            appearance = appearance,
            isSelectedAtCreation = isSelectedAtCreation,
        )

        val normal = createDrawable(appearance.normal, appearance)
        val pressed = createDrawable(resolvedPressed, appearance)
        val focused = createDrawable(appearance.focused, appearance)
        val disabled = createDrawable(appearance.disabled, appearance)
        val selected = createDrawable(appearance.selected, appearance)

        return ShapeDrawableSet(
            normal = normal,
            pressed = pressed,
            focused = focused,
            disabled = disabled,
            selected = selected,
            stateList = createStateList(
                appearance = appearance,
                resolvedPressed = resolvedPressed,
                normal = normal,
                pressed = pressed,
                focused = focused,
                disabled = disabled,
                selected = selected,
            ),
        )
    }

    private fun resolvePressedState(
        appearance: ShapeAppearance,
        isSelectedAtCreation: Boolean,
    ): ShapeStateAppearance {
        val configuredPressed = appearance.pressed
        if (!appearance.autoFillPressedColor) return configuredPressed

        val normalGradient = appearance.normal.gradient
        return when {
            // A configured normal gradient produces a dimmed pressed gradient.
            configuredPressed.gradient.startColor == UNSET_COLOR &&
                normalGradient.startColor != UNSET_COLOR -> {
                configuredPressed.copy(
                    gradient = ShapeGradient(
                        startColor = normalGradient.startColor.withAlphaFraction(PRESSED_ALPHA),
                        endColor = normalGradient.endColor.withAlphaFraction(PRESSED_ALPHA),
                    ),
                )
            }

            // Derive a pressed solid fill only when one was not explicitly supplied.
            configuredPressed.fillColor == UNSET_COLOR -> {
                val sourceColor = if (
                    isSelectedAtCreation && appearance.selected.fillColor != UNSET_COLOR
                ) {
                    appearance.selected.fillColor
                } else {
                    appearance.normal.fillColor
                }
                configuredPressed.copy(fillColor = sourceColor.withAlphaFraction(PRESSED_ALPHA))
            }

            else -> configuredPressed
        }
    }

    private fun createStateList(
        appearance: ShapeAppearance,
        resolvedPressed: ShapeStateAppearance,
        normal: GradientDrawable,
        pressed: GradientDrawable,
        focused: GradientDrawable,
        disabled: GradientDrawable,
        selected: GradientDrawable,
    ): StateListDrawable {
        return StateListDrawable().apply {
            // StateListDrawable uses the first matching state, so this order is significant.
            if (appearance.pressed.fillColor != UNSET_COLOR || resolvedPressed.gradient.isEnabled) {
                addState(intArrayOf(AndroidR.attr.state_pressed), pressed)
            }
            if (appearance.focused.strokeColor != UNSET_COLOR) {
                addState(intArrayOf(AndroidR.attr.state_focused), focused)
            }
            if (appearance.disabled.fillColor != UNSET_COLOR || appearance.disabled.gradient.isEnabled) {
                addState(intArrayOf(-AndroidR.attr.state_enabled), disabled)
            }
            if (
                appearance.selected.fillColor != UNSET_COLOR ||
                appearance.selected.strokeColor != UNSET_COLOR ||
                appearance.selected.gradient.isEnabled
            ) {
                addState(intArrayOf(AndroidR.attr.state_selected), selected)
            }
            addState(intArrayOf(), normal)
        }
    }

    private fun createDrawable(
        state: ShapeStateAppearance,
        appearance: ShapeAppearance,
    ): GradientDrawable {
        return if (state.gradient.isEnabled) {
            GradientDrawable(
                appearance.gradientOrientation.toPlatformOrientation(),
                state.gradient.toColorArray(),
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = appearance.corners.toPlatformRadii()
                gradientType = GradientDrawable.LINEAR_GRADIENT
                applyStroke(appearance.strokeWidth, state.strokeColor)
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = appearance.corners.toPlatformRadii()
                setColor(state.fillColor)
                applyStroke(appearance.strokeWidth, state.strokeColor)
            }
        }
    }

    private fun GradientDrawable.applyStroke(width: Int, color: Int) {
        if (width > 0) setStroke(width, color)
    }

    private fun ShapeGradient.toColorArray(): IntArray {
        return if (centerColor != UNSET_COLOR) {
            intArrayOf(startColor, centerColor, endColor)
        } else {
            intArrayOf(startColor, endColor)
        }
    }

    private fun ShapeCorners.toPlatformRadii(): FloatArray {
        return floatArrayOf(
            topLeft.toFloat(), topLeft.toFloat(),
            topRight.toFloat(), topRight.toFloat(),
            bottomRight.toFloat(), bottomRight.toFloat(),
            bottomLeft.toFloat(), bottomLeft.toFloat(),
        )
    }

    private fun ShapeGradientOrientation.toPlatformOrientation(): GradientDrawable.Orientation {
        return when (this) {
            ShapeGradientOrientation.LEFT_RIGHT -> GradientDrawable.Orientation.LEFT_RIGHT
            ShapeGradientOrientation.RIGHT_LEFT -> GradientDrawable.Orientation.RIGHT_LEFT
            ShapeGradientOrientation.TOP_BOTTOM -> GradientDrawable.Orientation.TOP_BOTTOM
            ShapeGradientOrientation.BOTTOM_TOP -> GradientDrawable.Orientation.BOTTOM_TOP
            ShapeGradientOrientation.TOP_LEFT_BOTTOM_RIGHT -> GradientDrawable.Orientation.TL_BR
            ShapeGradientOrientation.TOP_RIGHT_BOTTOM_LEFT -> GradientDrawable.Orientation.TR_BL
            ShapeGradientOrientation.BOTTOM_LEFT_TOP_RIGHT -> GradientDrawable.Orientation.BL_TR
            ShapeGradientOrientation.BOTTOM_RIGHT_TOP_LEFT -> GradientDrawable.Orientation.BR_TL
        }
    }

    /** Replaces the color alpha channel with a fraction of the full 0-255 range. */
    private fun Int.withAlphaFraction(opacity: Float): Int {
        return Color.argb(
            (255 * opacity).toInt(),
            Color.red(this),
            Color.green(this),
            Color.blue(this),
        )
    }

    private companion object {
        const val UNSET_COLOR = 0
        const val PRESSED_ALPHA = 0.5f
    }
}
