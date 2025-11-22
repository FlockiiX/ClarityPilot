package com.claritypilot

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.serialization.json.Json

val CANVAS_DATA_KEY = stringPreferencesKey("api_response_data")

data class RenderResult(
    val bodyBitmap: Bitmap? = null,
    val ctaBitmap: Bitmap? = null,
    val ctaLink: String? = null
)

class CanvasWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    private val jsonParser = Json { ignoreUnknownKeys = true }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val ctx = LocalContext.current
            val prefs = currentState<Preferences>()
            val rawData = prefs[CANVAS_DATA_KEY]

            val result by produceState(initialValue = RenderResult(), key1 = size, key2 = rawData) {
                try {
                    val response = if (rawData.isNullOrBlank()) {
                        getFriendlyState()
                    } else {
                        if (rawData.startsWith("ERROR:")) throw Exception(rawData.removePrefix("ERROR: "))
                        jsonParser.decodeFromString<WidgetResponse>(rawData)
                    }

                    val ctaElement = response.elements.find { it is WidgetElement.Cta } as? WidgetElement.Cta
                    val ctaHeightDp = 48f
                    val ctaMarginBottomDp = 16f
                    val totalCtaSpaceDp = if (ctaElement != null) ctaHeightDp + ctaMarginBottomDp + 8f else 0f

                    val bodyBmp = WidgetRenderer.renderBody(
                        ctx,
                        size,
                        response.elements,
                        paddingBottomDp = totalCtaSpaceDp
                    )

                    var ctaBmp: Bitmap? = null
                    if (ctaElement != null) {
                        ctaBmp = WidgetRenderer.renderCtaButton(ctx, size.width.value, ctaElement.content)
                    }

                    value = RenderResult(bodyBmp, ctaBmp, ctaElement?.link)

                } catch (e: Exception) {
                    Log.e("CanvasWidget", "Error", e)
                    val fallbackData = getFriendlyState()
                    val bmp = WidgetRenderer.renderBody(ctx, size, fallbackData.elements)
                    value = RenderResult(bodyBitmap = bmp)
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (result.bodyBitmap != null) {
                    Image(
                        provider = ImageProvider(result.bodyBitmap!!),
                        contentDescription = "Widget Content",
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionStartActivity<MainActivity>())
                    )
                }

                if (result.ctaBitmap != null && result.ctaLink != null) {
                    Image(
                        provider = ImageProvider(result.ctaBitmap!!),
                        contentDescription = "Action",
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(bottom = 16.dp)
                            .clickable(
                                actionStartActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(result.ctaLink))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            )
                    )
                }
            }
        }
    }

    private fun getFriendlyState(): WidgetResponse {
        return WidgetResponse(
            elements = listOf(
                WidgetElement.Spacer(),
                WidgetElement.Text(content = "ClarityPilot", fontStyle = "heading"),
                WidgetElement.Spacer(),
                WidgetElement.Text(content = "Ready for your next goal?", fontStyle = "normal")
            )
        )
    }
}