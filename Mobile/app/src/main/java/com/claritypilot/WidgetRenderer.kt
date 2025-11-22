package com.claritypilot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.unit.DpSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetRenderer {
    suspend fun renderBody(
        context: Context,
        size: DpSize,
        elements: List<WidgetElement>,
        paddingBottomDp: Float = 0f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
        val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)

        val paddingBottomPx = paddingBottomDp * density
        val usableHeightPx = (heightPx - paddingBottomPx).coerceAtLeast(1f)

        return withContext(Dispatchers.Default) {
            val bitmap = createBitmap(widthPx, heightPx)
            val canvas = Canvas(bitmap)

            bitmap.eraseColor(Color.TRANSPARENT)
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(140, 0, 0, 0)
                style = Paint.Style.FILL
            }
            val bgRect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

            val cornerRadius = 24f * density
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, scrimPaint)

            val padding = 16f * density
            val availableWidth = widthPx.toFloat() - (padding * 2)

            val bodyElements = elements.filter { it !is WidgetElement.Cta }
            val totalContentHeight = measureTotalHeight(context, bodyElements, availableWidth, density)

            val scale = if (totalContentHeight > usableHeightPx) usableHeightPx / totalContentHeight else 1.0f

            val scaledHeight = totalContentHeight * scale
            val startY = if (scaledHeight < usableHeightPx) (usableHeightPx - scaledHeight) / 2f else 0f

            canvas.withTranslation(padding, startY) {
                scale(scale, scale)
                translate((widthPx - (scale * widthPx)) / 2, 32f)
                var currentY = 0f
                bodyElements.forEach { element ->
                    currentY += drawElement(context, this, element, 0f, currentY, availableWidth, density, false)
                    currentY += 8f * density
                }
            }
            bitmap
        }
    }

    suspend fun renderCtaButton(context: Context, widthDp: Float, text: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
        val heightPx = (48 * density).toInt()

        return withContext(Dispatchers.Default) {
            val bitmap = createBitmap(widthPx, heightPx)
            val canvas = Canvas(bitmap)
            //bitmap.eraseColor(Color.BLACK)


            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
            val radius = heightPx / 2f
            canvas.drawRoundRect(rect, radius, radius, btnPaint)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 16f * density
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val textBounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = (heightPx / 2f) - textBounds.exactCenterY()

            canvas.drawText(text, widthPx / 2f, textY, textPaint)

            bitmap
        }
    }


    private fun measureTotalHeight(ctx: Context, elements: List<WidgetElement>, w: Float, d: Float): Float {
        var h = 0f
        val dummyCanvas = Canvas()
        elements.forEach { element ->
            h += drawElement(ctx, dummyCanvas, element, 0f, 0f, w, d, true)
            h += 8f * d
        }
        return h
    }

    private fun drawElement(
        ctx: Context, c: Canvas, el: WidgetElement, x: Float, y: Float, w: Float, d: Float,
        measureOnly: Boolean
    ): Float {
        return when (el) {
            is WidgetElement.Text -> drawText(el, c, x, y, w, d, measureOnly)
            is WidgetElement.Spacer -> 8f * d
            is WidgetElement.Card -> drawCard(ctx, c, el, x, y, w, d, measureOnly)
            is WidgetElement.Row -> drawRow(ctx, c, el, x, y, w, d, measureOnly)
            is WidgetElement.Cta -> 0f
        }
    }

    private fun drawText(el: WidgetElement.Text, c: Canvas, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean): Float {
        val defaultSize: Float
        val defaultColor: String
        val typeface: Typeface

        when (el.fontStyle) {
            "heading" -> {
                defaultSize = 24f
                defaultColor = "#FFFFFF"
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            "thin" -> {
                defaultSize = 13f
                defaultColor = "#9CA3AF"
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            }
            else -> {
                defaultSize = 16f
                defaultColor = "#E5E7EB"
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
        }

        val finalSize = defaultSize * d
        val finalColor = parseColor(defaultColor)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = finalColor
            textSize = finalSize
            setTypeface(typeface)
        }

        val alignment = Layout.Alignment.ALIGN_CENTER

        val layout = StaticLayout.Builder.obtain(el.content, 0, el.content.length, paint, w.toInt().coerceAtLeast(10))
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()

        if (!measureOnly) c.withTranslation(x, y) { layout.draw(this) }
        return layout.height.toFloat()
    }

    private fun drawCard(ctx: Context, c: Canvas, el: WidgetElement.Card, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean): Float {
        val pad = 16 * d
        val innerW = w - (pad * 2)
        var contentHeight = 0f
        el.content.forEach { child ->
            contentHeight += drawElement(ctx, c, child, 0f, 0f, innerW, d, true) + (8f * d)
        }

        if (!measureOnly) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseColor("#1E1E1E")
                style = Paint.Style.FILL
            }
            val rect = RectF(x, y, x + w, y + contentHeight + (pad * 2))
            c.drawRoundRect(rect, 16 * d, 16 * d, bgPaint)
        }

        if (!measureOnly) {
            var currentY = y + pad
            el.content.forEach { child ->
                currentY += drawElement(ctx, c, child, x + pad, currentY, innerW, d, false)
                currentY += 8f * d
            }
        }
        return contentHeight + (pad * 2)
    }

    private fun drawRow(ctx: Context, c: Canvas, el: WidgetElement.Row, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean): Float {
        if (el.columns.isEmpty()) return 0f

        val totalWeight = el.columns.sumOf { it.weight.toDouble() }.toFloat()
        val gap = 8f * d
        val totalGap = gap * (el.columns.size - 1)
        val usableWidth = w - totalGap

        val colWidths = el.columns.map {
            if (totalWeight > 0) (it.weight / totalWeight) * usableWidth else usableWidth / el.columns.size
        }

        val heights = FloatArray(el.columns.size)
        var maxHeight = 0f
        val dummyC = Canvas()

        el.columns.forEachIndexed { i, child ->
            heights[i] = drawElement(ctx, dummyC, child, 0f, 0f, colWidths[i], d, measureOnly = true)
            if (heights[i] > maxHeight) maxHeight = heights[i]
        }

        if (measureOnly) return maxHeight

        var currentX = x
        el.columns.forEachIndexed { i, child ->
            val offsetY = (maxHeight - heights[i]) / 2f
            drawElement(ctx, c, child, currentX, y + offsetY, colWidths[i], d, measureOnly = false)
            currentX += colWidths[i] + gap
        }

        return maxHeight
    }

    private fun parseColor(hex: String): Int {
        return try { Color.parseColor(hex) } catch (_: Exception) { Color.LTGRAY }
    }
}