package com.claritypilot


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.unit.DpSize
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import androidx.core.graphics.createBitmap

object WidgetRenderer {

    fun render(context: Context, size: DpSize, data: WidgetResponse): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
        val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)

        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bitmap)

        // 1. Background (Glassmorphism Scrim)
        bitmap.eraseColor(Color.TRANSPARENT)
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val bgRect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val cornerRadius = 24f * density
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, scrimPaint)

        // 2. Layout
        val padding = 16f * density
        val availableWidth = widthPx.toFloat() - (padding * 2)

        // 3. Measure & Scale
        val totalContentHeight = measureTotalHeight(context, data.elements, availableWidth, density)
        val scale = if (totalContentHeight > heightPx) heightPx.toFloat() / totalContentHeight else 1.0f

        val scaledHeight = totalContentHeight * scale
        val startY = if (scaledHeight < heightPx) (heightPx - scaledHeight) / 2f else 0f

        // 4. Draw
        canvas.withTranslation(padding, startY) {
            scale(scale, scale)

            var currentY = 0f
            data.elements.forEach { element ->
                currentY += drawElement(
                    context,
                    this,
                    element,
                    0f,
                    currentY,
                    availableWidth,
                    density
                )
                currentY += 8f * density
            }
        }

        return bitmap
    }

    private fun measureTotalHeight(ctx: Context, elements: List<WidgetElement>, w: Float, d: Float): Float {
        var h = 0f
        val dummyCanvas = Canvas()
        elements.forEach { element ->
            h += drawElement(ctx, dummyCanvas, element, 0f, 0f, w, d, measureOnly = true)
            h += 8f * d
        }
        return h
    }

    private fun drawElement(
        ctx: Context, c: Canvas, el: WidgetElement, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean = false
    ): Float {
        return when (el) {
            is WidgetElement.Text -> drawText(el, c, x, y, w, d, measureOnly)
            is WidgetElement.Spacer -> el.height * d
            is WidgetElement.Card -> drawCard(ctx, c, el, x, y, w, d, measureOnly)
            is WidgetElement.Row -> drawRow(ctx, c, el, x, y, w, d, measureOnly)
        }
    }

    private fun drawText(el: WidgetElement.Text, c: Canvas, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(el.color)
            textSize = el.size * d
            typeface = if (el.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val alignment = when (el.align) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val layout = StaticLayout.Builder.obtain(el.content, 0, el.content.length, paint, w.toInt().coerceAtLeast(10))
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()

        if (!measureOnly) {
            c.withTranslation(x, y) {
                layout.draw(this)
            }
        }
        return layout.height.toFloat()
    }

    private fun drawCard(ctx: Context, c: Canvas, el: WidgetElement.Card, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean): Float {
        val pad = el.padding * d
        val innerW = w - (pad * 2)
        var contentHeight = 0f
        el.content.forEach { child ->
            contentHeight += drawElement(ctx, c, child, 0f, 0f, innerW, d, true) + (8f * d)
        }

        if (!measureOnly) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseColor(el.color)
                style = Paint.Style.FILL
            }
            val rect = RectF(x, y, x + w, y + contentHeight + (pad * 2))
            c.drawRoundRect(rect, el.cornerRadius * d, el.cornerRadius * d, bgPaint)
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
        val colWidth = (w - ((el.columns.size - 1) * 8 * d)) / el.columns.size
        var maxHeight = 0f
        el.columns.forEachIndexed { index, child ->
            val childX = x + (index * (colWidth + 8 * d))
            val h = drawElement(ctx, c, child, childX, y, colWidth, d, measureOnly)
            if (h > maxHeight) maxHeight = h
        }
        return maxHeight
    }

    private fun parseColor(hex: String): Int {
        return try {
            hex.toColorInt()
        } catch (e: Exception) {
            Color.LTGRAY
        }
    }
}