package com.coachassist.services

import com.coachassist.models.LastUsedSettings
import com.coachassist.models.SavePlanRequest
import com.coachassist.models.SavedSessionPlan
import com.coachassist.models.SessionPlanSummary
import com.coachassist.models.UserProfile
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

class UserProfileService(private val db: Firestore) {

    companion object {
        private const val PROFILES_COLLECTION = "profiles"
        private const val SESSION_PLANS_COLLECTION = "session_plans"
    }

    suspend fun getProfileByEmail(email: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            // Use get() for Google Cloud Firestore
            val docRef = db.collection(PROFILES_COLLECTION).document(email).get().get()
            if (docRef.exists()) {
                // Manual mapping instead of toObject() to handle Kotlin serialization
                val data = docRef.data
                if (data != null) {
                    val lastUsedData = data["lastUsedSettings"] as? Map<String, Any>
                    val lastUsedSettings = lastUsedData?.let {
                        LastUsedSettings(
                            ageGroup = it["ageGroup"] as String,
                            focus = it["focus"] as String,
                            durationMinutes = (it["durationMinutes"] as Number).toInt(),
                            timeBreakdown = it["timeBreakdown"] as String,
                            numberOfPlayers = (it["numberOfPlayers"] as Number).toInt(),
                            extra = it["extra"] as? String ?: ""
                        )
                    }
                    
                    if (lastUsedSettings != null) {
                        UserProfile(
                            email = data["email"] as String,
                            name = data["name"] as String,
                            lastUsedSettings = lastUsedSettings
                        )
                    } else null
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            // Log the exception for debugging
            println("Error fetching profile: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun saveProfile(profile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        try {
            // Convert to a map for Firestore
            val profileMap = mapOf(
                "email" to profile.email,
                "name" to profile.name,
                "lastUsedSettings" to mapOf(
                    "ageGroup" to profile.lastUsedSettings.ageGroup,
                    "focus" to profile.lastUsedSettings.focus,
                    "durationMinutes" to profile.lastUsedSettings.durationMinutes,
                    "timeBreakdown" to profile.lastUsedSettings.timeBreakdown,
                    "numberOfPlayers" to profile.lastUsedSettings.numberOfPlayers,
                    "extra" to profile.lastUsedSettings.extra
                )
            )
            
            // Use get() for Google Cloud Firestore
            db.collection(PROFILES_COLLECTION).document(profile.email).set(profileMap).get()
            return@withContext profile
        } catch (e: Exception) {
            println("Error saving profile: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun saveSessionPlan(request: SavePlanRequest): SavedSessionPlan = withContext(Dispatchers.IO) {
        val userDocRef = db.collection(PROFILES_COLLECTION).document(request.email)
        if (!userDocRef.get().get().exists()) {
            throw NoSuchElementException("User with email ${request.email} not found.")
        }

        val planToSave = mapOf(
            "summary" to request.summary,
            "plan" to request.plan
        )

        val newPlanDocRef = userDocRef.collection(SESSION_PLANS_COLLECTION).add(planToSave).get()

        SavedSessionPlan(
            id = newPlanDocRef.id,
            summary = request.summary,
            plan = request.plan
        )
    }

    suspend fun getSessionPlanSummaries(email: String): List<SessionPlanSummary> = withContext(Dispatchers.IO) {
        val userDocRef = db.collection(PROFILES_COLLECTION).document(email)
        if (!userDocRef.get().get().exists()) {
            throw NoSuchElementException("User with email $email not found.")
        }

        // Use select() to only fetch the summary field for efficiency and lower cost
        val plansSnapshot = userDocRef.collection(SESSION_PLANS_COLLECTION).select("summary").get().get()

        plansSnapshot.documents.mapNotNull { doc ->
            doc.data?.let { data ->
                SessionPlanSummary(
                    id = doc.id,
                    summary = data["summary"] as String
                )
            }
        }
    }

    suspend fun getSavedSessionPlan(email: String, planId: String): SavedSessionPlan? = withContext(Dispatchers.IO) {
        val planDocRef = db.collection(PROFILES_COLLECTION).document(email)
            .collection(SESSION_PLANS_COLLECTION).document(planId)

        val docSnapshot = planDocRef.get().get()

        if (docSnapshot.exists()) {
            docSnapshot.data?.let { data ->
                SavedSessionPlan(
                    id = docSnapshot.id,
                    summary = data["summary"] as String,
                    plan = data["plan"] as String
                )
            }
        } else {
            null
        }
    }
}