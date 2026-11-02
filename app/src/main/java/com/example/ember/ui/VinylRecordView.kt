package com.example.ember.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * VinylRecordView
 *
 * A custom hardware-accelerated View rendering a hyper-realistic vinyl record:
 * - Jet-black base disc (#090909) with precision outer edge rim
 * - Authentic concentric micro-grooves with varying optical depth and spacing
 * - Realistic dual specular reflection sheen simulating studio overhead lighting
 * - Run-out groove border
 * - Center brass spindle ring (#D4AF37) and dark center hole
 */
class VinylRecordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#090909")
    }

    private val outerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1F1D24")
        strokeWidth = dpToPx(2f)
    }

    private val groovePaints = listOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#161519")
            strokeWidth = dpToPx(0.8f)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#1F1E24")
            strokeWidth = dpToPx(1.2f)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#121115")
            strokeWidth = dpToPx(0.6f)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#26242E")
            strokeWidth = dpToPx(1.4f)
        }
    )

    private val runOutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#141318")
        strokeWidth = dpToPx(3f)
    }

    private val sheenPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val sheenPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val goldSpindlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#D4AF37")
        strokeWidth = dpToPx(2.5f)
    }

    private val centerHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#070709")
    }

    private val sheenPath1 = Path()
    private val sheenPath2 = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f

        // Studio light sheen gradient across opposing quadrants
        sheenPaint1.shader = LinearGradient(
            cx - radius, cy - radius,
            cx + radius, cy + radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(22, 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.15f, 0.5f, 0.85f),
            Shader.TileMode.CLAMP
        )

        sheenPaint2.shader = LinearGradient(
            cx + radius, cy - radius,
            cx - radius, cy + radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(16, 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.2f, 0.5f, 0.8f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - dpToPx(2f)
        if (radius <= 0) return

        // 1. Solid Jet-Black Disc Base
        canvas.drawCircle(cx, cy, radius, basePaint)

        // 2. Outer Rim Border
        canvas.drawCircle(cx, cy, radius, outerRimPaint)

        // 3. Concentric Vinyl Micro-Grooves
        val innerGrooveR = radius * 0.44f
        val outerGrooveR = radius * 0.95f
        val grooveStep = (outerGrooveR - innerGrooveR) / 46f

        for (i in 0 until 46) {
            val r = innerGrooveR + i * grooveStep
            val paint = groovePaints[i % groovePaints.size]
            canvas.drawCircle(cx, cy, r, paint)
        }

        // 4. Smooth Run-Out Groove Ring near center label
        canvas.drawCircle(cx, cy, innerGrooveR, runOutPaint)

        // 5. Specular Lighting Sheens
        canvas.drawCircle(cx, cy, radius, sheenPaint1)
        canvas.drawCircle(cx, cy, radius, sheenPaint2)

        // 6. Central Spindle Detail (Gold Brass Ring + Deep Dark Hole)
        val spindleR = dpToPx(10f)
        canvas.drawCircle(cx, cy, spindleR, centerHolePaint)
        canvas.drawCircle(cx, cy, spindleR, goldSpindlePaint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
