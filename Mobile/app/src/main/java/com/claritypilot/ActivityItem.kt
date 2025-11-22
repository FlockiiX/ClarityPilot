package com.claritypilot

import kotlinx.serialization.Serializable // Wichtig: Import hinzufügen

// Beschreibt einen einzelnen Aktivitäts-Eintrag
@Serializable
data class ActivityItem(
    val type: String,
    val label: String,
    val duration: String,
    val icon: String,
    val iconSize: Int= 32,
)

// Wrapper für die komplette Timeline, die aus den Tages-Einträgen besteht.
// Wir verwenden eine Map, um dynamische Schlüssel wie "Today", "Yesterday" zu verarbeiten.
typealias TimelineData = Map<String, List<ActivityItem>>