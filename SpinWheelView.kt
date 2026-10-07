package com.spinearn.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class SpinWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    val prizes = listOf(10, 25, 50, 100, 250, 500, 1000, 50)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private var rotation = 0f
    private var spinning = false

    fun spin(onResult: (Int) -> Unit) {
        if (spinning) return
        spinning = true
        val index = (prizes.indices).random()
        val segment = 360f / prizes.size
        val target = 360f * 5 + (360f - (index + 0.5f) * segment)
        val start = rotation
        val end = rotation + target
        ValueAnimator.ofFloat(start, end).apply {
            duration = 3600L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { rotation = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rotation %= 360f
                    spinning = false
                    onResult(prizes[index])
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - 12f
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val colors = intArrayOf(0xFF6D28D9.toInt(), 0xFF1E40AF.toInt(), 0xFF7C3AED.toInt(), 0xFFB45309.toInt())
        for (i in prizes.indices) {
            paint.color = colors[i % colors.size]
            canvas.drawArc(rect, i * 45f, 45f, true, paint)
            val angle = Math.toRadians(i * 45.0 + 22.5)
            val tx = cx + (radius * 0.64f) * cos(angle).toFloat()
            val ty = cy + (radius * 0.64f) * sin(angle).toFloat() + 10f
            canvas.drawText("${prizes[i]}", tx, ty, textPaint)
        }
        canvas.restore()

        paint.color = 0xFFF6C453.toInt()
        canvas.drawCircle(cx, cy, radius * 0.20f, paint)
        textPaint.color = 0xFF171321.toInt()
        textPaint.textSize = radius * 0.09f
        canvas.drawText("SPIN", cx, cy + textPaint.textSize / 3f, textPaint)
        textPaint.color = 0xFFFFFFFF.toInt()

        // Pointer
        paint.color = 0xFFFFFFFF.toInt()
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - radius - 2)
            lineTo(cx - 18, cy - radius + 32)
            lineTo(cx + 18, cy - radius + 32)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
