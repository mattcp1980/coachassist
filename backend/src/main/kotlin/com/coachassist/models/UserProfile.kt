package com.coachassist.models

import kotlinx.serialization.Serializable

@Serializable
data class LastUsedSettings(
    val ageGroup: String,
    val focus: String,
    val durationMinutes: Int,
    val timeBreakdown: String,
    val numberOfPlayers: Int
)

@Serializable
data class UserProfile(
    val email: String,
    val lastUsedSettings: LastUsedSettings
)