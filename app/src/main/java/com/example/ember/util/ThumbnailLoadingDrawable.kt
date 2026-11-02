package com.example.ember.util

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

/**
 * ThumbnailLoadingDrawable - Custom animated loading drawable for Coil image requests.
 *
 * Renders a dark obsidian card base with an animated rotating accent arc
 * in EMBER's signature glowing orange. Eliminates static/wrong-song placeholders.
 */
class ThumbnailLoadingDrawable(
    private val strokeWidthDp: Float = 2.5f,
    private val arcRadiusDp: Float = 14f
) : Drawable(), Animatable {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#12121A")
        style = Paint.Style.FILL
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var rotationAngle = 0f
    private val boundsRect = RectF()
    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 950L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            invalidateSelf()
        }
    }

    init {
        start()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        // 1. Draw clean dark card background
        boundsRect.set(b)
        canvas.drawRect(boundsRect, bgPaint)

        // 2. Calculate responsive dimensions based on view size
        val density = canvas.density.let { if (it > 0) it / 160f else 2.5f }
        val stroke = (strokeWidthDp * density).coerceAtLeast(2f)
        val maxRadius = (minOf(b.width(), b.height()) * 0.32f)
        val radius = (arcRadiusDp * density).coerceAtMost(maxRadius).coerceAtLeast(8f)

        arcPaint.strokeWidth = stroke
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val colors = intArrayOf(
            Color.parseColor("#00FF5A36"),
            Color.parseColor("#44FF5A36"),
            Color.parseColor("#FFFF5A36")
        )
        val positions = floatArrayOf(0.0f, 0.45f, 1.0f)
        arcPaint.shader = SweepGradient(cx, cy, colors, positions)

        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawArc(arcRect, 0f, 290f, false, arcPaint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
        arcPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
        arcPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        if (!animator.isStarted) {
            animator.start()
        }
    }

    override fun stop() {
        animator.cancel()
    }

    override fun isRunning(): Boolean = animator.isRunning
}
