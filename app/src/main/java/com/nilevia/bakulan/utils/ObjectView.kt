package com.nilevia.bakulan.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.NumberFormat
import kotlin.math.max

class ObjectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.holo_orange_dark)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = TEXT_SIZE
    }

    private var results: List<Detection> = emptyList()
    private var scaleFactor: Float = 1f
    private val bounds = Rect()

    fun setResults(detectionResults: List<Detection>, imageHeight: Int, imageWidth: Int) {
        results = detectionResults
        scaleFactor = max(width / imageWidth.toFloat(), height / imageHeight.toFloat())
        invalidate() // Redraw the view with updated results
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results.forEach { result ->
            drawBoundingBox(canvas, result)
            drawLabel(canvas, result)
        }
    }

    private fun drawBoundingBox(canvas: Canvas, result: Detection) {
        val (left, top, right, bottom) = with(result.boundingBox) {
            listOf(left * scaleFactor, top * scaleFactor, right * scaleFactor, bottom * scaleFactor)
        }

        val drawableRect = RectF(left, top, right, bottom)
        canvas.drawRect(drawableRect, boxPaint)
    }

    private fun drawLabel(canvas: Canvas, result: Detection) {
        val label = result.categories[0].label
        val score = NumberFormat.getPercentInstance().format(result.categories[0].score)
        val drawableText = "$label $score"

        textPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()

        val left = result.boundingBox.left * scaleFactor
        val top = result.boundingBox.top * scaleFactor

        canvas.drawRect(
            left,
            top,
            left + textWidth + TEXT_PADDING,
            top + textHeight + TEXT_PADDING,
            textBackgroundPaint
        )
        canvas.drawText(drawableText, left, top + textHeight, textPaint)
    }

    fun clear() {
        results = emptyList()
        invalidate() // Clear the view by re-rendering with no results
    }

    companion object {
        private const val TEXT_SIZE = 50f
        private const val TEXT_PADDING = 8f
    }
}
