package com.claritypilot

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp // WICHTIG: Dieser Import hat gefehlt
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.claritypilot.MainActivity
import kotlinx.serialization.json.Json

val CANVAS_DATA_KEY = stringPreferencesKey("api_response_data")

// Kleiner Helper Data Class um Body und CTA zu trennen
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

            // Wir produzieren jetzt ein kombiniertes Resultat
            val result by produceState<RenderResult>(initialValue = RenderResult(), key1 = size, key2 = rawData) {
                try {
                    val response = if (rawData.isNullOrBlank()) {
                        getFriendlyState()
                    } else {
                        if (rawData.startsWith("ERROR:")) {
                            throw Exception(rawData.removePrefix("ERROR: "))
                        }
                        jsonParser.decodeFromString<WidgetResponse>(rawData)
                    }

                    // 1. CTA finden und extrahieren
                    val ctaElement = response.elements.find { it is WidgetElement.Cta } as? WidgetElement.Cta

                    // 2. Body rendern
                    // Wir ziehen etwas Höhe ab für den Button, falls einer da ist
                    val buttonHeightDp = if (ctaElement != null) 60f else 0f // 48dp button + margin

                    // FIX: Korrekte Syntax für DpSize mit Subtraktion
                    val bodySize = androidx.compose.ui.unit.DpSize(
                        size.width,
                        size.height - buttonHeightDp.dp
                    )

                    val bodyBmp = WidgetRenderer.renderBody(ctx, bodySize, response.elements)

                    // 3. CTA Button rendern (falls vorhanden)
                    var ctaBmp: Bitmap? = null
                    if (ctaElement != null) {
                        ctaBmp = WidgetRenderer.renderCtaButton(ctx, size.width.value, ctaElement.content)
                    }

                    value = RenderResult(bodyBmp, ctaBmp, ctaElement?.link)

                } catch (e: Exception) {
                    Log.e("CanvasWidget", "Error", e)
                    // Fallback rendern
                    val fallbackData = getFriendlyState()
                    val bmp = WidgetRenderer.renderBody(ctx, size, fallbackData.elements)
                    value = RenderResult(bodyBitmap = bmp)
                }
            }

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Main Content (Klick öffnet APP)
                if (result.bodyBitmap != null) {
                    Image(
                        provider = ImageProvider(result.bodyBitmap!!),
                        contentDescription = "Widget Content",
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight() // Nimmt restlichen Platz
                            .clickable(actionStartActivity<MainActivity>())
                    )
                }

                // 2. CTA Button (Klick öffnet LINK)
                if (result.ctaBitmap != null && result.ctaLink != null) {
                    Spacer(modifier = GlanceModifier.height(8.dp)) // Kleiner Abstand
                    Image(
                        provider = ImageProvider(result.ctaBitmap!!),
                        contentDescription = "Let's Go",
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(48.dp) // Feste Höhe wie im Renderer definiert
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
                WidgetElement.Text(
                    content = "ClarityPilot",
                    fontStyle = "heading",
                ),
                WidgetElement.Spacer(),
                WidgetElement.Text(
                    content = "Ready for your next goal?",
                    fontStyle = "normal",
                )
            )
        )
    }
}