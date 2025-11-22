package com.claritypilot


import android.content.Context
import android.util.Log
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.serialization.json.Json

val CANVAS_DATA_KEY = stringPreferencesKey("api_response_data")

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

            val bitmap = remember(size, rawData) {
                val data = try {
                    if (rawData.isNullOrBlank()) {
                        getFriendlyState("We finalize your next tip..")
                    } else {
                        if (rawData.startsWith("ERROR:")) {
                            throw Exception(rawData.removePrefix("ERROR: "))
                        }
                        jsonParser.decodeFromString<WidgetResponse>(rawData)
                    }
                } catch (e: Exception) {
                    Log.e("CanvasWidget", "Rendering fallback", e)
                    getFriendlyState("We finalize your next tip")
                }
                WidgetRenderer.render(ctx, size, data)
            }

            Box(modifier = GlanceModifier.fillMaxSize()) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = "Widget Content",
                    modifier = GlanceModifier.fillMaxSize()
                )
            }
        }
    }

    private fun getFriendlyState(title: String): WidgetResponse {
        return WidgetResponse(
            elements = listOf(
                WidgetElement.Spacer(height = 16f),
                WidgetElement.Text(
                    content = title,
                    size = 22f,
                    color = "#FFFFFF",
                    isBold = true,
                    align = "center"
                ),
                WidgetElement.Spacer(height = 8f),
                WidgetElement.Text(
                    content = "We are currently working on the next great idea for you.",
                    size = 14f,
                    color = "#AAAAAA",
                    align = "center"
                ),
                WidgetElement.Spacer(height = 4f),
                WidgetElement.Text(
                    content = "We'll be back in a few seconds.",
                    size = 12f,
                    color = "#666666",
                    align = "center"
                )
            )
        )
    }
}