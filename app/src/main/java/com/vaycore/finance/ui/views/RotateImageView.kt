package com.vaycore.finance.ui.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView

class RotateImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var animator: ObjectAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // set pivot to image center
        pivotX = w / 2f
        pivotY = h / 2f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRotate()
    }

    override fun onDetachedFromWindow() {
        stopRotate()
        super.onDetachedFromWindow()
    }

    fun startRotate(duration: Long = 1500L) {
        if (animator?.isRunning == true) return

        animator = ObjectAnimator.ofFloat(
            this,
            View.ROTATION,
            0f,
            360f
        ).apply {
            this.duration = duration
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun stopRotate() {
        animator?.cancel()
    }
}