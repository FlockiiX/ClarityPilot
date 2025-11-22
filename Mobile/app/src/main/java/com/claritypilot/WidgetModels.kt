package com.claritypilot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WidgetResponse(
    @SerialName("bg_color") val backgroundColor: String = "#000000",
    val elements: List<WidgetElement>
)

@Serializable
sealed class WidgetElement {

    @Serializable
    @SerialName("text")
    data class Text(
        val content: String,
        val size: Float = 16f,
        val color: String = "#FFFFFF",
        val isBold: Boolean = false,
        val align: String = "left"
    ) : WidgetElement()

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        val height: Float
    ) : WidgetElement()

    @Serializable
    @SerialName("card")
    data class Card(
        val color: String = "#1E1E1E",
        val cornerRadius: Float = 16f,
        val padding: Float = 12f,
        val content: List<WidgetElement>
    ) : WidgetElement()

    @Serializable
    @SerialName("row")
    data class Row(
        val columns: List<WidgetElement>
    ) : WidgetElement()
}