package com.example.signor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import java.util.*

class Display(context: Context?, attrs: AttributeSet?) : DisplayContainer(context, attrs) {
    private var frequency = 0.0
    private var level = 0.0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val w = parentWidth
        val h = parentHeight / 5
        setMeasuredDimension(w, h)
    }

    fun setFrequency(frequency: Double) {
        this.frequency = frequency

        invalidate()
    }

    fun setLevel(level: Double) {
        this.level = level

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val frequencyText = String.format(Locale.getDefault(), "%5.2fHz", frequency)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = (height / 2).toFloat()
        paint.textScaleX = 0.9f
        paint.color = textColour
        paint.style = Paint.Style.FILL_AND_STROKE
        canvas.drawText(frequencyText, MARGIN.toFloat(), height.toFloat(), paint)

        val dbText = String.format(Locale.getDefault(), "%5.2fdB", level)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dbText, (width - MARGIN).toFloat(), height.toFloat(), paint)
    }
}