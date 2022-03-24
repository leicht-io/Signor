package com.example.signor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

class SiriVisualView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var phase = 0f
    private var amplitude = 1.5f // wave height
    private var frequency = 1.2f // number of waves
    private var idleAmplitude = 0.05f // default height
    private var numberOfWaves = 1.0f // number of wave lines
    private var phaseShift = -0.1f // wave speed
    private var primaryWaveLineWidth = 4.0f // outer line stroke
    private var secondaryWaveLineWidth = 0.5f // inner line stroke
    private var density = 5f
    var mPaintColor: Paint = Paint()
    var isStraightLine = false

    fun updateViewColor(@ColorInt color: Int) {
        mPaintColor.color = color
    }

    fun updateSpeaking(isSpeaking: Boolean) {
        isStraightLine = isSpeaking
    }

    fun updateAmplitude(ampli: Float) {
        amplitude = Math.max(ampli, idleAmplitude)
    }

    fun updateSpeed(phase: Float) {
        phaseShift = phase
    }

    /** Here you can override default wave values and customize com.example.signor.SiriVisualView

    updateNumberOfWaves()
    updatePrimaryLineStroke()
    . . .
     */

    override fun onDraw(canvas: Canvas) {
        if (isStraightLine) {
            var i = 0
            while (i < numberOfWaves) {
                mPaintColor.strokeWidth =
                    if (i == 0) primaryWaveLineWidth else secondaryWaveLineWidth
                val halfHeight = height / 2.toFloat()
                val width = width.toFloat()
                val mid = width / 2.toFloat()
                val maxAmplitude = halfHeight - 4.0f
                val progress = 1.0f - i.toFloat() / numberOfWaves
                val normedAmplitude = (1.5f * progress - 0.5f) * amplitude
                val path = Path()
                val multiplier = Math.min(1.0f, progress / 3.0f * 2.0f + 1.0f / 3.0f)
                var x = 0f
                while (x < width + density) {
                    // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
                    val scaling = (-Math.pow(
                        1 / mid * (x - mid).toDouble(),
                        2.0
                    ) + 1).toFloat()
                    val y =
                        (scaling * maxAmplitude * normedAmplitude * Math.sin(2 * Math.PI * (x / width) * frequency + phase) + halfHeight).toFloat()
                    if (x == 0f) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    x += density
                }
                mPaintColor.style = Paint.Style.STROKE
                mPaintColor.isAntiAlias = true
                canvas.drawPath(path, mPaintColor)
                i++
            }
        } else {
            for (i in 5 downTo -5 step 5) {
                canvas.drawLine(
                    i.toFloat(),
                    height / 2.toFloat(),
                    width.toFloat(),
                    height / 2.toFloat(),
                    mPaintColor
                )
            }
        }
        phase += phaseShift
        invalidate()
    }
}