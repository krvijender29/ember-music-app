package com.example.ember.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * EMBER Waveform Buffer View - Animated Equalizer Spectrum Bars.
 *
 * Simulates a live audiophile equalizer bouncing across multiple frequency bands
 * with staggered phases. Used inside the Full Player buffering pill, Search Tab
 * loading indicator, and Home feed skeleton header.
 */
class EmberWaveformBufferView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val barCount = 4
    private var phase = 0f

    var primaryColor: Int = Color.parseColor("#FF5A36")
        set(value) {
            field = value
            updateShader()
            invalidate()
        }

    var secondaryColor: Int = Color.parseColor("#FFA048")
        set(value) {
            field = value
            updateShader()
            invalidate()
        }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barRect = RectF()

    private val animator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 850L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        updateShader()
    }

    private fun updateShader() {
        if (height > 0) {
            barPaint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                secondaryColor, primaryColor,
                Shader.TileMode.CLAMP
            )
        } else {
            barPaint.color = primaryColor
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barWidth = 2.8f * density
        val barGap = 3.2f * density
        val totalBarsWidth = (barCount * barWidth) + ((barCount - 1) * barGap)
        val startX = (w - totalBarsWidth) / 2f
        val centerY = h / 2f

        val minHeight = 4f * density
        val maxHeight = (h - 2f * density).coerceAtLeast(minHeight + 2f)

        for (i in 0 until barCount) {
            // Stagger phase per bar
            val barPhase = phase + (i * 1.15f)
            val normalizedHeight = ((sin(barPhase) + 1.0) / 2.0).toFloat()
            val currentBarHeight = minHeight + (maxHeight - minHeight) * normalizedHeight

            val left = startX + (i * (barWidth + barGap))
            val top = centerY - (currentBarHeight / 2f)
            val right = left + barWidth
            val bottom = centerY + (currentBarHeight / 2f)

            barRect.set(left, top, right, bottom)
            val cornerRadius = barWidth / 2f
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == View.VISIBLE && !animator.isStarted) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (animator.isStarted) {
            animator.cancel()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE && isAttachedToWindow) {
            if (!animator.isStarted) animator.start()
        } else {
            if (animator.isStarted) animator.cancel()
        }
    }
}
