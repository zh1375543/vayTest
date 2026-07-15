package com.vaycore.finance.ui.widget.shape

import android.R as AndroidR
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Applies ShapeView text colors and compound drawable state changes to a TextView.
 *
 * The controller keeps the XML-defined compound drawables so the default set can
 * be restored after a selected state is cleared.
 */
internal class TextStateController(
    private val target: TextView,
) {

    private var appearance: TextStateAppearance? = null
    private var defaultDrawables: Array<Drawable?> = emptyArray()

    /** Reads the current TextView drawables before any selected-state replacement. */
    fun apply(textAppearance: TextStateAppearance) {
        appearance = textAppearance
        defaultDrawables = target.compoundDrawablesRelative.copyOf()
        applyTextColors(textAppearance)
    }

    /** Updates the selected compound drawables while preserving unspecified positions. */
    fun updateSelectedState(isSelected: Boolean) {
        val textAppearance = appearance ?: return
        if (isSelected) {
            target.setCompoundDrawablesRelativeWithIntrinsicBounds(
                textAppearance.selectedDrawables.startResId.toDrawableOrDefault(START),
                textAppearance.selectedDrawables.topResId.toDrawableOrDefault(TOP),
                textAppearance.selectedDrawables.endResId.toDrawableOrDefault(END),
                textAppearance.selectedDrawables.bottomResId.toDrawableOrDefault(BOTTOM),
            )
        } else {
            target.setCompoundDrawablesRelativeWithIntrinsicBounds(
                defaultDrawables.getOrNull(START),
                defaultDrawables.getOrNull(TOP),
                defaultDrawables.getOrNull(END),
                defaultDrawables.getOrNull(BOTTOM),
            )
        }
    }

    /** Applies XML-defined compound drawable bounds after the TextView is measured. */
    fun updateDrawableBounds() {
        val size = appearance?.drawableSize ?: return
        if (size.width <= 0 && size.height <= 0) return

        val width = size.width.takeIf { it > 0 }
        val height = size.height.takeIf { it > 0 }
        target.compoundDrawablesRelative.forEach { drawable ->
            drawable?.setBounds(
                0,
                0,
                width ?: drawable.intrinsicWidth,
                height ?: drawable.intrinsicHeight,
            )
        }
        target.invalidate()
    }

    private fun applyTextColors(textAppearance: TextStateAppearance) {
        val states = mutableListOf<IntArray>()
        val colors = mutableListOf<Int>()

        if (textAppearance.pressedTextColor != UNSET_COLOR) {
            states += intArrayOf(AndroidR.attr.state_pressed)
            colors += textAppearance.pressedTextColor
        }
        if (textAppearance.selectedTextColor != UNSET_COLOR) {
            states += intArrayOf(AndroidR.attr.state_selected)
            colors += textAppearance.selectedTextColor
        }
        if (textAppearance.disabledTextColor != UNSET_COLOR) {
            states += intArrayOf(-AndroidR.attr.state_enabled)
            colors += textAppearance.disabledTextColor
        }
        if (states.isEmpty()) return

        states += intArrayOf()
        colors += target.currentTextColor
        target.setTextColor(ColorStateList(states.toTypedArray(), colors.toIntArray()))
    }

    private fun Int.toDrawableOrDefault(index: Int): Drawable? {
        return if (this != NO_RESOURCE) {
            ContextCompat.getDrawable(target.context, this)
        } else {
            defaultDrawables.getOrNull(index)
        }
    }

    private companion object {
        const val START = 0
        const val TOP = 1
        const val END = 2
        const val BOTTOM = 3
        const val NO_RESOURCE = 0
        const val UNSET_COLOR = 0
    }
}
