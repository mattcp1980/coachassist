package com.coachassist.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val email: String,
    val name: String,
    val lastUsedSettings: LastUsedSettings
)

@Serializable
data class LastUsedSettings(
    val ageGroup: String,
    val focus: String,
    val durationMinutes: Int,
    val timeBreakdown: String,
    val numberOfPlayers: Int,
    val extra: String = ""
)