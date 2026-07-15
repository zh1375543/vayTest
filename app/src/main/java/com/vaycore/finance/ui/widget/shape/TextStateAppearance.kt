package com.vaycore.finance.ui.widget.shape

/**
 * TextView-specific ShapeView attributes.
 *
 * The normal text color is intentionally not stored here because it comes from
 * android:textColor and must be read from the target TextView during rendering.
 */
/**
 * Shape attributes used only by text-based views.
 * Background-only views do not need to consume this model.
 */
internal data class TextStateAppearance(
    val pressedTextColor: Int,
    val selectedTextColor: Int,
    val disabledTextColor: Int,
    val selectedDrawables: SelectedDrawables,
    val drawableSize: CompoundDrawableSize,
)

/** Resource IDs that replace compound drawables while the TextView is selected. */
internal data class SelectedDrawables(
    val startResId: Int,
    val topResId: Int,
    val endResId: Int,
    val bottomResId: Int,
)

/** Optional XML-defined compound drawable bounds in pixels. */
internal data class CompoundDrawableSize(
    val width: Int,
    val height: Int,
)
