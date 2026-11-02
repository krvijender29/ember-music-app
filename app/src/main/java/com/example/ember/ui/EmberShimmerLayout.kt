package com.example.ember.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout

/**
 * EMBER Shimmer Layout - Ultra-smooth, hardware-accelerated skeleton shimmer.
 *
 * Utilizes PorterDuff.Mode.SRC_ATOP on a hardware layer to cast a sweeping
 * liquid-specular light beam exclusively across child placeholder silhouettes,
 * preserving deep obsidian dark backgrounds and crisp rounded margins.
 */
class EmberShimmerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val shimmerWidthPx = 280f * density
    private var shimmerProgress = 0f
    private val shaderMatrix = Matrix()

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private var shimmerShader: LinearGradient? = null

    var isShimmering: Boolean = true
        private set

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1350L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            shimmerProgress = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val colors = intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#15FFFFFF"),
                Color.parseColor("#45FFFFFF"),
                Color.parseColor("#15FFFFFF"),
                Color.TRANSPARENT
            )
            val positions = floatArrayOf(0.0f, 0.35f, 0.50f, 0.65f, 1.0f)
            shimmerShader = LinearGradient(
                0f, 0f, shimmerWidthPx, 0f,
                colors, positions,
                Shader.TileMode.CLAMP
            )
            shimmerPaint.shader = shimmerShader
        }
    }

    fun startShimmer() {
        if (!isShimmering) {
            isShimmering = true
            if (isAttachedToWindow && visibility == View.VISIBLE && !animator.isStarted) {
                animator.start()
            }
            invalidate()
        }
    }

    fun stopShimmer() {
        isShimmering = false
        if (animator.isStarted) {
            animator.cancel()
        }
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!isShimmering || width <= 0 || height <= 0 || shimmerShader == null) {
            super.dispatchDraw(canvas)
            return
        }

        // 1. Render all child placeholder views into a compositing layer
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)

        // 2. Compute smooth diagonal translation across the entire layout
        val totalDistance = width + height + shimmerWidthPx
        val translateX = -shimmerWidthPx + (shimmerProgress * totalDistance)

        shaderMatrix.reset()
        shaderMatrix.postRotate(20f, shimmerWidthPx / 2f, height / 2f)
        shaderMatrix.postTranslate(translateX, 0f)
        shimmerShader?.setLocalMatrix(shaderMatrix)

        // 3. Draw shimmer beam constrained to child shapes by SRC_ATOP
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
        canvas.restoreToCount(save)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isShimmering && visibility == View.VISIBLE && !animator.isStarted) {
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
        if (visibility == View.VISIBLE && isShimmering && isAttachedToWindow) {
            if (!animator.isStarted) animator.start()
        } else {
            if (animator.isStarted) animator.cancel()
        }
    }
}
