package com.claritypilot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.unit.DpSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.max
import kotlin.math.min

object WidgetRenderer {
    private val imageCache = LruCache<String, Bitmap>(10 * 1024 * 1024)

    suspend fun render(context: Context, size: DpSize, data: WidgetResponse): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (size.width.value * density).toInt().coerceAtLeast(1)
        val heightPx = (size.height.value * density).toInt().coerceAtLeast(1)

        val loadedImages = loadImagesForData(context, data)

        return withContext(Dispatchers.Default) {
            val bitmap = createBitmap(widthPx, heightPx)
            val canvas = Canvas(bitmap)

            // 2. Background (Glassmorphism Scrim)
            bitmap.eraseColor(Color.TRANSPARENT)
            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(140, 0, 0, 0)
                style = Paint.Style.FILL
            }
            val bgRect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
            val cornerRadius = 24f * density
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, scrimPaint)

            // 3. Layout
            val padding = 16f * density
            val availableWidth = widthPx.toFloat() - (padding * 2)

            // 4. Measure & Scale
            val totalContentHeight = measureTotalHeight(context, data.elements, availableWidth, density)
            val scale = if (totalContentHeight > heightPx) heightPx.toFloat() / totalContentHeight else 1.0f

            val scaledHeight = totalContentHeight * scale
            val startY = if (scaledHeight < heightPx) (heightPx - scaledHeight) / 2f else 0f

            // 5. Draw
            canvas.withTranslation(padding, startY) {
                scale(scale, scale)

                var currentY = 0f
                data.elements.forEach { element ->
                    currentY += drawElement(
                        context, this, element, 0f, currentY, availableWidth, density,
                        loadedImages = loadedImages,
                        measureOnly = false,
                    )
                    currentY += 8f * density
                }
            }
            bitmap
        }
    }

    private suspend fun loadImagesForData(context: Context, data: WidgetResponse): Map<String, Bitmap> = withContext(Dispatchers.IO) {
        val urls = mutableSetOf<String>()
        collectUrls(data.elements, urls)

        urls.map { url ->
            async {
                url to (getCachedOrLoad(context, url))
            }
        }.awaitAll().toMap().filterValues { it != null } as Map<String, Bitmap>
    }

    private fun collectUrls(elements: List<WidgetElement>, urls: MutableSet<String>) {
        elements.forEach {
            when (it) {
                is WidgetElement.Image -> urls.add(it.url)
                is WidgetElement.Card -> collectUrls(it.content, urls)
                is WidgetElement.Row -> collectUrls(it.columns, urls)
                else -> {}
            }
        }
    }


    private fun getCachedOrLoad(context: Context, source: String): Bitmap? {
        imageCache.get(source)?.let { return it }

        return try {
            val bitmap = if (source.startsWith("http")) {
                val bytes = URL(source).readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                var resId = context.resources.getIdentifier(source, "drawable", context.packageName)
                if (resId == 0) {
                    resId = context.resources.getIdentifier(source, "mipmap", context.packageName)
                }

                if (resId != 0) {
                    BitmapFactory.decodeResource(context.resources, resId)
                } else {
                    Log.e("WidgetRenderer", "Resource not found: $source")
                    null
                }
            }

            if (bitmap != null) imageCache.put(source, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.e("WidgetRenderer", "Failed to load image: $source", e)
            null
        }
    }

    private fun measureTotalHeight(ctx: Context, elements: List<WidgetElement>, w: Float, d: Float): Float {
        var h = 0f
        val dummyCanvas = Canvas()
        elements.forEach { element ->
            h += drawElement(ctx, dummyCanvas, element, 0f, 0f, w, d, measureOnly = true, loadedImages = emptyMap())
            h += 8f * d
        }
        return h
    }

    private fun drawElement(
        ctx: Context, c: Canvas, el: WidgetElement, x: Float, y: Float, w: Float, d: Float,
        measureOnly: Boolean, loadedImages: Map<String, Bitmap>
    ): Float {
        return when (el) {
            is WidgetElement.Text -> drawText(el, c, x, y, w, d, measureOnly)
            is WidgetElement.Spacer -> el.height * d
            is WidgetElement.Image -> drawImage(el, c, x, y, w, d, measureOnly, loadedImages)
            is WidgetElement.Card -> drawCard(ctx, c, el, x, y, w, d, measureOnly, loadedImages)
            is WidgetElement.Row -> drawRow(ctx, c, el, x, y, w, d, measureOnly, loadedImages)
        }
    }

    private fun drawImage(
        el: WidgetElement.Image, c: Canvas, x: Float, y: Float, w: Float, d: Float,
        measureOnly: Boolean, loadedImages: Map<String, Bitmap>
    ): Float {
        val targetHeight = el.height * d

        if (!measureOnly) {
            val bitmap = loadedImages[el.url]
            if (bitmap != null) {
                c.save()

                val path = Path()
                val rect = RectF(x, y, x + w, y + targetHeight)
                path.addRoundRect(rect, el.cornerRadius * d, el.cornerRadius * d, Path.Direction.CW)
                c.clipPath(path)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val matrix = Matrix()

                val srcW = bitmap.width.toFloat()
                val srcH = bitmap.height.toFloat()

                val scale: Float
                val dx: Float
                val dy: Float

                if (el.scaleType == "fit") {
                    scale = min(w / srcW, targetHeight / srcH)

                    dx = x + (w - srcW * scale) / 2f
                    dy = y + (targetHeight - srcH * scale) / 2f

                } else {
                    scale = max(w / srcW, targetHeight / srcH)

                    dx = x + (w - srcW * scale) / 2f
                    dy = y + (targetHeight - srcH * scale) / 2f
                }

                matrix.setScale(scale, scale)
                matrix.postTranslate(dx, dy)

                c.drawBitmap(bitmap, matrix, paint)

                c.restore()
            } else {
                val paint = Paint().apply { color = Color.DKGRAY }
                c.drawRoundRect(RectF(x, y, x + w, y + targetHeight), el.cornerRadius*d, el.cornerRadius*d, paint)
            }
        }
        return targetHeight
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
            c.withTranslation(x, y) { layout.draw(this) }
        }
        return layout.height.toFloat()
    }

    private fun drawCard(ctx: Context, c: Canvas, el: WidgetElement.Card, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean, loadedImages: Map<String, Bitmap>): Float {
        val pad = el.padding * d
        val innerW = w - (pad * 2)
        var contentHeight = 0f
        el.content.forEach { child ->
            contentHeight += drawElement(ctx, c, child, 0f, 0f, innerW, d, true, loadedImages) + (8f * d)
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
                currentY += drawElement(ctx, c, child, x + pad, currentY, innerW, d, false, loadedImages)
                currentY += 8f * d
            }
        }
        return contentHeight + (pad * 2)
    }

    private fun drawRow(ctx: Context, c: Canvas, el: WidgetElement.Row, x: Float, y: Float, w: Float, d: Float, measureOnly: Boolean, loadedImages: Map<String, Bitmap>): Float {
        if (el.columns.isEmpty()) return 0f
        val colWidth = (w - ((el.columns.size - 1) * 8 * d)) / el.columns.size
        var maxHeight = 0f
        el.columns.forEachIndexed { index, child ->
            val childX = x + (index * (colWidth + 8 * d))
            val h = drawElement(ctx, c, child, childX, y, colWidth, d, measureOnly, loadedImages)
            if (h > maxHeight) maxHeight = h
        }
        return maxHeight
    }

    private fun parseColor(hex: String): Int {
        return try {
            hex.toColorInt()
        } catch (_: Exception) {
            Color.LTGRAY
        }
    }
}