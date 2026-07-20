package com.vaycore.finance.ui.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.vaycore.finance.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin

private val amountAnimators = WeakHashMap<TextView, ValueAnimator>()

fun View.resetScale() {
    clearAnimation()
    scaleX = 1f
    scaleY = 1f
}

fun View.stopScaleAnimation() {
    clearAnimation()
    scaleX = 1f
    scaleY = 1f
}

class BigDecimalEvaluator : TypeEvaluator<BigDecimal> {
    override fun evaluate(
        fraction: Float,
        startValue: BigDecimal,
        endValue: BigDecimal,
    ): BigDecimal {
        return startValue + (endValue - startValue) * fraction.toBigDecimal()
    }
}

fun TextView.animateAmount(
    targetAmount: BigDecimal?,
    duration: Long = 1000L,
    prefix: String = "",
) {
    val target = targetAmount ?: BigDecimal.ZERO
    val fractionDigits = target.stripTrailingZeros().scale().coerceIn(0, 2)
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }

    amountAnimators.remove(this)?.cancel()
    val animator = ValueAnimator.ofObject(BigDecimalEvaluator(), BigDecimal.ZERO, target).apply {
        this.duration = duration
        addUpdateListener {
            val current = it.animatedValue as BigDecimal
            this@animateAmount.text = "$prefix${formatter.format(current)}"
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (amountAnimators[this@animateAmount] === animation) {
                    amountAnimators.remove(this@animateAmount)
                }
            }
        })
    }
    amountAnimators[this] = animator
    animator.start()
}

fun View.startArcMotionAnimation() {
    val radius = context.resources.getDimension(R.dimen.dp_155)
    val centerX = this.width / 2
    val centerY = this.height / 2

    val startAngle = 180f
    val endAngle = 180f + 135f  // 315 degrees

    val animator = ValueAnimator.ofFloat(startAngle, endAngle)
    animator.duration = 1500
    animator.interpolator = AccelerateDecelerateInterpolator()

    animator.addUpdateListener { animation ->
        val angle = animation.animatedValue as Float
        val angleRad = Math.toRadians(angle.toDouble())

        val x = centerX + radius * cos(angleRad).toFloat()
        val y = centerY + radius * sin(angleRad).toFloat()

        this.x = (x - this.width / 2f)
        this.y = (y - this.height / 2f)
    }

    animator.start()
}
