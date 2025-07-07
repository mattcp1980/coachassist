package com.coachassist.models

import kotlinx.serialization.Serializable

@Serializable
data class SavedSessionPlan(
    val id: String? = null, // Populated from Firestore document ID
    val summary: String,
    val plan: String
)

@Serializable
data class SavePlanRequest(
    val email: String,
    val summary: String,
    val plan: String
)

@Serializable
data class SessionPlanSummary(
    val id: String,
    val summary: String
)