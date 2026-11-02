package com.example.ember.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * EMBER Laser Sweep View - High-speed glowing laser beam for indeterminate line buffering.
 *
 * Casts a bidirectional glowing laser streamer across the bottom edge of the Mini Player
 * during network audio buffering, delivering immediate visual feedback before playback begins.
 */
class EmberLaserSweepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val beamLengthPx = 110f * density
    private var sweepProgress = 0f
    private val shaderMatrix = Matrix()

    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var laserShader: LinearGradient? = null

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1050L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            sweepProgress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val colors = intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#44FF5A36"),
                Color.parseColor("#FFFF5A36"),
                Color.WHITE,
                Color.parseColor("#FFFF5A36"),
                Color.parseColor("#44FF5A36"),
                Color.TRANSPARENT
            )
            val positions = floatArrayOf(0.0f, 0.20f, 0.45f, 0.50f, 0.55f, 0.80f, 1.0f)

            laserShader = LinearGradient(
                0f, 0f, beamLengthPx, 0f,
                colors, positions,
                Shader.TileMode.CLAMP
            )
            laserPaint.shader = laserShader
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || laserShader == null) return

        val travelDistance = w + beamLengthPx
        val translateX = -beamLengthPx + (sweepProgress * travelDistance)

        shaderMatrix.reset()
        shaderMatrix.postTranslate(translateX, 0f)
        laserShader?.setLocalMatrix(shaderMatrix)

        canvas.drawRect(0f, 0f, w, h, laserPaint)
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
