package com.coachassist.services

import com.coachassist.models.UserProfile
import com.coachassist.models.LastUsedSettings
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileService(private val db: Firestore) {

    suspend fun getProfileByEmail(email: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("profiles").document(email).get().get()
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
                            numberOfPlayers = (it["numberOfPlayers"] as Number).toInt()
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
                    "numberOfPlayers" to profile.lastUsedSettings.numberOfPlayers
                )
            )
            
            db.collection("profiles").document(profile.email).set(profileMap).get()
            return@withContext profile
        } catch (e: Exception) {
            println("Error saving profile: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}