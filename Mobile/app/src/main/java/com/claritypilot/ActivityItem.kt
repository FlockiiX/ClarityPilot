package com.claritypilot

import kotlinx.serialization.Serializable

@Serializable
data class ActivityItem(
    val type: String,
    val label: String,
    val duration: String,
    val icon: String,
    val iconSize: Int= 32,
)

typealias TimelineData = Map<String, List<ActivityItem>>