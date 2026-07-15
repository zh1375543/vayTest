package com.vaycore.finance.ui.widget.shape

import android.view.View

/**
 * Owns the drawable set attached to one View.
 *
 * The controller is deliberately small: it applies a factory result and keeps
 * the result available for later bounds updates. It does not parse XML or build
 * drawables.
 */
internal class ShapeBackgroundController(
    private val target: View,
    private val drawableFactory: ShapeDrawableFactory = ShapeDrawableFactory(),
) {

    private var drawableSet: ShapeDrawableSet? = null

    /** Applies a new background and records its state drawables for resizing. */
    fun apply(appearance: ShapeAppearance) {
        drawableSet = drawableFactory.create(
            appearance = appearance,
            isSelectedAtCreation = target.isSelected,
        ).also { set ->
            target.background = set.stateList
        }
    }

    /** Must be called from the owning View's onSizeChanged callback. */
    fun updateBounds(width: Int, height: Int) {
        drawableSet?.updateBounds(width, height)
    }
}
