package com.coachassist.models

import kotlinx.serialization.Serializable

@Serializable
data class SessionPlanRequest(
    val ageGroup: String,
    val focus: String,
    val durationMinutes: Int,
    val timeBreakdown: String,
    val numberOfPlayers: Int,
    val extra: String = ""
)