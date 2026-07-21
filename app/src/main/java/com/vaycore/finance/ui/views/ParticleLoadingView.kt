package com.vaycore.finance.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.vaycore.finance.R
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A lightweight particle-orbit loading indicator.
 *
 * Particles are emitted from a point moving around the center, then drift and fade to form
 * a soft comet-like trail. Animation is tied to the view lifecycle so hidden loading states
 * do not keep rendering in the background.
 */
class ParticleLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var alpha: Float = 0f,
        var fadeStep: Float = 0f,
        var scale: Float = 1f
    )

    private val particles = ArrayList<Particle>(MAX_PARTICLES)
    private val recycledParticles = ArrayDeque<Particle>(MAX_PARTICLES)
    private val shaderMatrix = Matrix()

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    @ColorInt
    private var particleColor = ContextCompat.getColor(context, R.color.color_7087F8)

    private var orbitAngle = 0f
    private var particleRadius = 0f
    private var particleShader: RadialGradient? = null
    private var isAttached = false
    private val animationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }

    init {
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = dp(DEFAULT_SIZE_DP)
        val measuredWidth = resolveSize(desiredSize, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredSize, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        particleRadius = (min(width, height) * PARTICLE_RADIUS_RATIO).coerceAtLeast(dp(1.2f))
        particleShader = RadialGradient(
            0f,
            0f,
            particleRadius,
            intArrayOf(particleColor, particleColor, android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        particlePaint.shader = particleShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val outerRadius = min(width, height) / 2f
        val orbitRadius = outerRadius * ORBIT_RADIUS_RATIO

        val radians = Math.toRadians(orbitAngle.toDouble())
        val emitterX = centerX + orbitRadius * cos(radians).toFloat()
        val emitterY = centerY + orbitRadius * sin(radians).toFloat()

        if (shouldAnimate()) {
            emitParticles(emitterX, emitterY)
        }
        drawParticles(canvas)

        if (shouldAnimate()) {
            updateParticles()
            orbitAngle = (orbitAngle + ANGLE_STEP) % 360f
            postInvalidateOnAnimation()
        }
    }

    private fun emitParticles(x: Float, y: Float) {
        repeat(PARTICLES_PER_FRAME) {
            if (particles.size >= MAX_PARTICLES) return
            val particle = if (recycledParticles.isEmpty()) Particle() else recycledParticles.removeLast()
            particle.x = x
            particle.y = y
            particle.velocityX = randomRange(-DRIFT_SPEED, DRIFT_SPEED)
            particle.velocityY = randomRange(-DRIFT_SPEED, DRIFT_SPEED)
            particle.alpha = randomRange(0.78f, 1f)
            particle.fadeStep = randomRange(0.012f, 0.022f)
            particle.scale = randomRange(0.55f, 1.15f)
            particles.add(particle)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        val shader = particleShader ?: return
        for (particle in particles) {
            shaderMatrix.reset()
            shaderMatrix.setScale(particle.scale, particle.scale)
            shaderMatrix.postTranslate(particle.x, particle.y)
            shader.setLocalMatrix(shaderMatrix)
            particlePaint.alpha = (particle.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(particle.x, particle.y, particleRadius * particle.scale, particlePaint)
        }
        particlePaint.alpha = 255
    }

    private fun updateParticles() {
        var index = particles.lastIndex
        while (index >= 0) {
            val particle = particles[index]
            particle.x += particle.velocityX
            particle.y += particle.velocityY
            particle.alpha -= particle.fadeStep
            if (particle.alpha <= MIN_VISIBLE_ALPHA) {
                particles.removeAt(index)
                recycledParticles.addLast(particle)
            }
            index--
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        isAttached = false
        clearParticles()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) postInvalidateOnAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) postInvalidateOnAnimation()
    }

    private fun shouldAnimate(): Boolean =
        animationsEnabled && isAttached && isShown && windowVisibility == VISIBLE

    private fun clearParticles() {
        recycledParticles.addAll(particles)
        particles.clear()
    }

    private fun randomRange(min: Float, max: Float): Float =
        min + Random.nextFloat() * (max - min)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val DEFAULT_SIZE_DP = 64
        const val MAX_PARTICLES = 180
        const val PARTICLES_PER_FRAME = 3
        const val PARTICLE_RADIUS_RATIO = 0.038f
        const val ORBIT_RADIUS_RATIO = 0.68f
        const val ANGLE_STEP = 3f
        const val DRIFT_SPEED = 0.55f
        const val MIN_VISIBLE_ALPHA = 0.04f
    }
}
