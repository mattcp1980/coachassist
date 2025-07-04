package com.coachassist.models

import kotlinx.serialization.Serializable

@Serializable
data class SessionRequest(
    val focus: String,
    val durationMinutes: Int,
    val timeBreakdown: String, // e.g., "25/50/25"
    val numberOfPlayers: Int? = null
)

@Serializable
data class SessionResponse(
    val focus: String,
    val totalDurationMinutes: Int,
    val activities: List<Activity>
)

@Serializable
data class Activity(
    val name: String,
    val description: String,
    val durationMinutes: Int
)