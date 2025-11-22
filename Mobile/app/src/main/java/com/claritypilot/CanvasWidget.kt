package com.claritypilot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

val CANVAS_DATA_KEY = stringPreferencesKey("api_response_data")

class CanvasWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val context = LocalContext.current

            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val apiData = prefs[CANVAS_DATA_KEY] ?: "Waiting..."

            val bitmap = remember(size, apiData) {
                drawDynamicContent(context, size, apiData)
            }

            Box(modifier = GlanceModifier.fillMaxSize()) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
            }
        }
    }

    private fun drawDynamicContent(context: Context, size: DpSize, text: String): Bitmap {
        val widthPx = size.width.value.toInt() * context.resources.displayMetrics.density.toInt()
        val heightPx = size.height.value.toInt() * context.resources.displayMetrics.density.toInt()

        if (widthPx <= 0 || heightPx <= 0) return createBitmap(1, 1)

        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = "#121212".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), 32f, 32f, bgPaint)

        val circlePaint = Paint().apply {
            color = if (text.length % 2 == 0) Color.CYAN else Color.MAGENTA
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawCircle(
            (widthPx / 2).toFloat(),
            (heightPx / 2).toFloat(),
            (widthPx.coerceAtMost(heightPx) / 4).toFloat(),
            circlePaint
        )

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            text,
            (widthPx / 2).toFloat(),
            (heightPx / 2).toFloat() + 100,
            textPaint
        )

        return bitmap
    }
}