package com.example.ember.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * EMBER Audiophile 5-Band Vertical Equalizer Slider
 * Custom touch-optimized, hardware-accelerated vertical gain slider.
 * Range: -12dB (bottom) to +12dB (top) with tactile center 0dB notch marker.
 */
class VerticalEqSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val minDb: Int = -12
    val maxDb: Int = 12
    var currentDb: Int = 0
        private set

    var isBypassed: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onGainChanged: ((Int) -> Unit)? = null
    var onGainChangeFinished: (() -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private val trackWidthPx = 4f * density
    private val thumbRadiusPx = 8.5f * density
    private val thumbHaloRadiusPx = 13.5f * density
    private val notchWidthPx = 14f * density
    private val notchHeightPx = 2f * density
    private val verticalPaddingPx = 15f * density

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1FFFFFFF")
    }

    private val notchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44FFFFFF")
    }

    private val activeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF5A36")
    }

    private val thumbHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#45FF5A36")
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF5A36")
    }

    private val thumbCenterPipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFFF")
    }

    private val trackRect = RectF()
    private val activeRect = RectF()
    private var lastHapticDb: Int? = null

    fun setGain(db: Int, notify: Boolean = false) {
        val clamped = db.coerceIn(minDb, maxDb)
        if (clamped != currentDb) {
            currentDb = clamped
            invalidate()
            if (notify) {
                onGainChanged?.invoke(currentDb)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defWidth = (48f * density).toInt()
        val defHeight = (135f * density).toInt()
        val width = resolveSize(defWidth, widthMeasureSpec)
        val height = resolveSize(defHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val topY = verticalPaddingPx
        val bottomY = height - verticalPaddingPx
        val centerY = (topY + bottomY) / 2f
        val travelLength = bottomY - topY

        // 1. Inactive Track Groove (Full height rounded bar)
        trackRect.set(cx - trackWidthPx / 2f, topY, cx + trackWidthPx / 2f, bottomY)
        canvas.drawRoundRect(trackRect, trackWidthPx / 2f, trackWidthPx / 2f, trackPaint)

        // 2. Center 0dB Notch Marker
        val notchLeft = cx - notchWidthPx / 2f
        val notchRight = cx + notchWidthPx / 2f
        val notchTop = centerY - notchHeightPx / 2f
        val notchBottom = centerY + notchHeightPx / 2f
        canvas.drawRoundRect(RectF(notchLeft, notchTop, notchRight, notchBottom), 1f * density, 1f * density, notchPaint)

        // 3. Current Thumb Position
        val fraction = (currentDb - minDb).toFloat() / (maxDb - minDb).toFloat()
        val thumbY = bottomY - (fraction * travelLength)

        // 4. Active Fill (From 0dB Center to Thumb)
        val activeColor = if (isBypassed) Color.parseColor("#5571717A") else Color.parseColor("#FF5A36")
        activeFillPaint.color = activeColor

        if (Math.abs(thumbY - centerY) > 1f) {
            val fillTop = Math.min(centerY, thumbY)
            val fillBottom = Math.max(centerY, thumbY)
            activeRect.set(cx - trackWidthPx / 2f, fillTop, cx + trackWidthPx / 2f, fillBottom)
            canvas.drawRoundRect(activeRect, trackWidthPx / 2f, trackWidthPx / 2f, activeFillPaint)
        }

        // 5. Thumb Outer Amber Glow
        val haloColor = if (isBypassed) Color.parseColor("#1571717A") else Color.parseColor("#45FF5A36")
        thumbHaloPaint.color = haloColor
        canvas.drawCircle(cx, thumbY, thumbHaloRadiusPx, thumbHaloPaint)

        // 6. Thumb Circular Cap
        val thumbCol = if (isBypassed) Color.parseColor("#8E8E93") else Color.parseColor("#FF5A36")
        thumbPaint.color = thumbCol
        canvas.drawCircle(cx, thumbY, thumbRadiusPx, thumbPaint)

        // 7. Center White Specular Pip
        val pipCol = if (isBypassed) Color.parseColor("#C7C7CC") else Color.parseColor("#FFFFFF")
        thumbCenterPipPaint.color = pipCol
        canvas.drawCircle(cx, thumbY, 2.5f * density, thumbCenterPipPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || isBypassed) {
            return false
        }

        val topY = verticalPaddingPx
        val bottomY = height - verticalPaddingPx
        val travelLength = bottomY - topY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromTouchY(event.y, topY, bottomY, travelLength)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateFromTouchY(event.y, topY, bottomY, travelLength)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onGainChangeFinished?.invoke()
                lastHapticDb = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouchY(touchY: Float, topY: Float, bottomY: Float, travelLength: Float) {
        val clampedY = touchY.coerceIn(topY, bottomY)
        val fraction = 1.0f - ((clampedY - topY) / travelLength)
        val calculatedDb = Math.round(minDb + fraction * (maxDb - minDb))

        if (calculatedDb != currentDb) {
            currentDb = calculatedDb

            // Tactile haptic click when snapping/crossing 0dB notch
            if (currentDb == 0 && lastHapticDb != 0) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            lastHapticDb = currentDb

            invalidate()
            onGainChanged?.invoke(currentDb)
        }
    }
}
