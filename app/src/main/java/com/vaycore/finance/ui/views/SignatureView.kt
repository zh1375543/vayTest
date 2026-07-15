package com.vaycore.finance.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.vaycore.finance.R
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class SignatureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnSignatureListener {
        /** User started signing (first stroke) */
        fun onStartSigning()
        /** User cleared signature, currently empty */
        fun onCleared()
    }

    private var listener: OnSignatureListener? = null
    fun setOnSignatureListener(l: OnSignatureListener) {
        listener = l
    }

    // whether a signature exists
    private var hasSignature = false

    // bounding box of current strokes
    private var dirtyLeft = Float.MAX_VALUE
    private var dirtyTop = Float.MAX_VALUE
    private var dirtyRight = Float.MIN_VALUE
    private var dirtyBottom = Float.MIN_VALUE

    private val paint = Paint().apply {
        color = Color.BLACK           // black pen
        isAntiAlias = true
        strokeWidth = context.resources.getDimension(R.dimen.dp_8)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private var bitmap: Bitmap? = null
    private var canvasBitmap: Canvas? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap == null) {
            bitmap = createBitmap(w, h)
            canvasBitmap = Canvas(bitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!hasSignature) {
                    hasSignature = true
                    listener?.onStartSigning()
                }
                path.moveTo(event.x, event.y)
                markDirty(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(event.x, event.y)
                markDirty(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                canvasBitmap?.drawPath(path, paint)
                path.reset()
            }
        }
        invalidate()
        return true
    }

    private fun markDirty(x: Float, y: Float) {
        // account for half stroke width to include full stroke
        val halfStroke = paint.strokeWidth / 2
        dirtyLeft   = minOf(dirtyLeft,   x - halfStroke)
        dirtyTop    = minOf(dirtyTop,    y - halfStroke)
        dirtyRight  = maxOf(dirtyRight,  x + halfStroke)
        dirtyBottom = maxOf(dirtyBottom, y + halfStroke)
    }

    /**
     * Get a cropped bitmap containing only the signature strokes
     */
    fun getCroppedSignatureBitmap(): Bitmap? {
        val bmp = bitmap ?: return null
        if (dirtyLeft == Float.MAX_VALUE) return null  // no strokes

        val left   = dirtyLeft.coerceAtLeast(0f).toInt()
        val top    = dirtyTop.coerceAtLeast(0f).toInt()
        val right  = dirtyRight.coerceAtMost(bmp.width.toFloat()).toInt()
        val bottom = dirtyBottom.coerceAtMost(bmp.height.toFloat()).toInt()
        val width  = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        // create new bitmap with white background
        val resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)

        // white background
        canvas.drawColor(Color.WHITE)

        // draw cropped area
        val cropped = Bitmap.createBitmap(bmp, left, top, width, height)
        canvas.drawBitmap(cropped, 0f, 0f, null)

        // scale down by half
        val scaledWidth = width / 2
        val scaledHeight = height / 2
        return resultBitmap.scale(scaledWidth.coerceAtLeast(1), scaledHeight.coerceAtLeast(1))
    }


    fun clear() {
        // clear bitmap and path
        bitmap = createBitmap(width, height)
        canvasBitmap = Canvas(bitmap!!)
        path.reset()
        invalidate()
        if (hasSignature) {
            hasSignature = false
            listener?.onCleared()
        }
        // reset bounding box
        dirtyLeft = Float.MAX_VALUE
        dirtyTop = Float.MAX_VALUE
        dirtyRight = Float.MIN_VALUE
        dirtyBottom = Float.MIN_VALUE
    }

    fun getSignatureBitmap(): Bitmap? {
        val currentBitmap = bitmap ?: return null
        val resultBitmap = createBitmap(currentBitmap.width, currentBitmap.height)
        val canvas = Canvas(resultBitmap)

        // draw white background first
        canvas.drawColor(Color.WHITE)
        // then draw signature content
        canvas.drawBitmap(currentBitmap, 0f, 0f, null)

        return resultBitmap
    }


    /**
     * Save cropped signature strokes to file
     */
    fun saveToFile(file: File): Boolean {
        val cropped = getCroppedSignatureBitmap() ?: return false

        try {
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            FileOutputStream(file).use { out ->
                val success = cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                return success
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}