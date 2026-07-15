package com.vaycore.finance.ui.widget.shape

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Output created for a ShapeAppearance.
 *
 * Keeping each GradientDrawable is intentional: diagonal gradients can render
 * incorrectly after a size change unless every state drawable receives updated
 * bounds. This model makes that requirement explicit and keeps the renderer
 * stateless.
 */
internal data class ShapeDrawableSet(
    val normal: GradientDrawable,
    val pressed: GradientDrawable,
    val focused: GradientDrawable,
    val disabled: GradientDrawable,
    val selected: GradientDrawable,
    val stateList: StateListDrawable,
) {

    /** Updates all state drawables, not only the StateListDrawable wrapper. */
    fun updateBounds(width: Int, height: Int) {
        allDrawables.forEach { drawable ->
            drawable.setBounds(0, 0, width, height)
        }
        stateList.setBounds(0, 0, width, height)
    }

    private val allDrawables: List<GradientDrawable>
        get() = listOf(normal, pressed, focused, disabled, selected)
}
