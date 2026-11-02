package com.example.ember.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * EMBER Radial Buffer Ring - Radiant Sci-Fi Orbiting Laser Ring.
 *
 * Designed for Full Player and Mini Player play/pause buffering states.
 * Features a high-speed rotating sweep gradient arc, breathing radial halo glow,
 * and a white-hot leading spark bead.
 */
class EmberRadialBufferView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // Configurable visual parameters
    var primaryColor: Int = Color.parseColor("#FF5A36")
        set(value) {
            field = value
            updatePaints()
            invalidate()
        }

    var showAmbientGlow: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // Animation state
    private var rotationAngle = 0f
    private var glowPulse = 0.5f

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1FFFFFFF")
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val sparkHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arcBounds = RectF()

    // Animators
    private val rotateAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 900L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidate()
        }
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0.35f, 0.95f).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            glowPulse = it.animatedValue as Float
        }
    }

    init {
        updatePaints()
    }

    private fun updatePaints() {
        val red = Color.red(primaryColor)
        val green = Color.green(primaryColor)
        val blue = Color.blue(primaryColor)

        sparkHaloPaint.color = Color.argb(190, red, green, blue)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val minDim = min(w, h).toFloat()
        val stroke = if (minDim > 55f * density) 3.5f * density else 2.4f * density

        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke

        val radius = (minDim / 2f) - (stroke / 2f) - (3f * density)
        val cx = w / 2f
        val cy = h / 2f

        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Radial glow shader
        val glowRadius = minDim * 0.52f
        val glowColors = intArrayOf(
            Color.argb(70, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
            Color.argb(25, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
            Color.TRANSPARENT
        )
        glowPaint.shader = RadialGradient(cx, cy, glowRadius, glowColors, floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP)

        // Sweep gradient for the rotating arc
        val arcColors = intArrayOf(
            Color.TRANSPARENT,
            Color.argb(40, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
            Color.argb(160, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
            primaryColor
        )
        val arcPositions = floatArrayOf(0.0f, 0.40f, 0.75f, 1.0f)
        arcPaint.shader = SweepGradient(cx, cy, arcColors, arcPositions)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val stroke = arcPaint.strokeWidth
        val radius = (minDim / 2f) - (stroke / 2f) - (3f * density)

        // 1. Ambient breathing halo glow
        if (showAmbientGlow) {
            canvas.save()
            glowPaint.alpha = (glowPulse * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, radius + (6f * density), glowPaint)
            canvas.restore()
        }

        // 2. Faint glass background circle track
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 3. Rotating Laser Arc (270 degrees sweep)
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawArc(arcBounds, 0f, 270f, false, arcPaint)

        // 4. Leading spark bead at the tip of the arc (270 degrees)
        val tipRad = Math.toRadians(270.0)
        val sparkX = cx + radius * cos(tipRad).toFloat()
        val sparkY = cy + radius * sin(tipRad).toFloat()

        val haloR = stroke * 1.55f
        val coreR = stroke * 0.72f
        canvas.drawCircle(sparkX, sparkY, haloR, sparkHaloPaint)
        canvas.drawCircle(sparkX, sparkY, coreR, sparkPaint)

        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == View.VISIBLE) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE && isAttachedToWindow) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        if (!rotateAnimator.isStarted) rotateAnimator.start()
        if (!pulseAnimator.isStarted) pulseAnimator.start()
    }

    private fun stopAnimation() {
        if (rotateAnimator.isStarted) rotateAnimator.cancel()
        if (pulseAnimator.isStarted) pulseAnimator.cancel()
    }
}
