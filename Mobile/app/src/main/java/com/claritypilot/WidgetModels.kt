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
    abstract val weight: Float

    @Serializable
    @SerialName("text")
    data class Text(
        val content: String,
        val fontStyle: String = "normal",
        override val weight: Float = 1f
    ) : WidgetElement()

    @Serializable
    @SerialName("cta")
    data class Cta(
        val content: String,
        val link: String,
        override val weight: Float = 1f
    ) : WidgetElement()

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        override val weight: Float = 1f
    ) : WidgetElement()

    @Serializable
    @SerialName("card")
    data class Card(
        val content: List<WidgetElement>,
        override val weight: Float = 1f
    ) : WidgetElement()

    @Serializable
    @SerialName("row")
    data class Row(
        val columns: List<WidgetElement>,
        override val weight: Float = 1f
    ) : WidgetElement()
}