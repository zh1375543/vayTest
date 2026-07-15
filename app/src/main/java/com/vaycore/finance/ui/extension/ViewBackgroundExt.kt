package com.vaycore.finance.ui.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View

private fun Drawable.rotateFixedSize(context: Context, angle: Float, sizeDp: Float): Drawable {
    val sizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        sizeDp,
        context.resources.displayMetrics
    ).toInt()

    val bitmap = toBitmap(sizePx, sizePx)
    val matrix = Matrix().apply { postRotate(angle) }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    return BitmapDrawable(context.resources, rotatedBitmap).apply {
        setBounds(0, 0, sizePx, sizePx) // fixed 24dp size
    }
}

private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

fun View.setRoundedRectangleBackground(
    solidColor: Int = 0x00000000,
    radius: Float = 0f,
    strokeColor: Int = 0x00000000,
    strokeWidth: Float = 0f,
    leftTopRadius: Float = radius,
    rightTopRadius: Float = radius,
    rightBottomRadius: Float = radius,
    leftBottomRadius: Float = radius,
) {
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = floatArrayOf(
            leftTopRadius, leftTopRadius,
            rightTopRadius, rightTopRadius,
            rightBottomRadius, rightBottomRadius,
            leftBottomRadius, leftBottomRadius
        )
        setColor(solidColor)
        if (strokeWidth > 0) {
            setStroke(strokeWidth.toInt(), strokeColor)
        }
    }
}

/**
 * Set gradient background with corner radius support
 *
 * @param startColor gradient start color
 * @param centerColor gradient center color (optional)
 * @param endColor gradient end color
 * @param angle gradient angle (default 90°)
 * @param orientation gradient orientation (default top to bottom)
 * @param radius corner radius in pixels, default 0 (no rounding)
 * @param leftTopRadius top-left corner radius, defaults to radius
 * @param rightTopRadius top-right corner radius, defaults to radius
 * @param rightBottomRadius bottom-right corner radius, defaults to radius
 * @param leftBottomRadius bottom-left corner radius, defaults to radius
 */
fun View.setLinearGradientBackground(
    startColor: Int,
    centerColor: Int? = null,
    endColor: Int,
    angle: Float = 90f,
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM,
    radius: Float = 0f,
    leftTopRadius: Float = radius,
    rightTopRadius: Float = radius,
    rightBottomRadius: Float = radius,
    leftBottomRadius: Float = radius,
) {
    val gradientDrawable =
        GradientDrawable(orientation, intArrayOf(startColor, centerColor ?: startColor, endColor))

    // set gradient angle
    gradientDrawable.orientation = when (angle) {
        0f -> GradientDrawable.Orientation.LEFT_RIGHT
        45f -> GradientDrawable.Orientation.TL_BR
        90f -> GradientDrawable.Orientation.TOP_BOTTOM
        135f -> GradientDrawable.Orientation.TR_BL
        180f -> GradientDrawable.Orientation.RIGHT_LEFT
        225f -> GradientDrawable.Orientation.BR_TL
        270f -> GradientDrawable.Orientation.BOTTOM_TOP
        315f -> GradientDrawable.Orientation.BL_TR
        else -> GradientDrawable.Orientation.TOP_BOTTOM // default
    }

    gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
//    gradientDrawable.useLevel = true

    // set corner radius
    gradientDrawable.cornerRadii = floatArrayOf(
        leftTopRadius, leftTopRadius,
        rightTopRadius, rightTopRadius,
        rightBottomRadius, rightBottomRadius,
        leftBottomRadius, leftBottomRadius
    )

    background = gradientDrawable
}
