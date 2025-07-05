package com.coachassist.services

import com.coachassist.models.UserProfile
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileService(private val db: Firestore) {

    suspend fun getProfileByEmail(email: String): UserProfile? = withContext(Dispatchers.IO) {
        val docRef = db.collection("profiles").document(email).get().get()
        if (docRef.exists()) {
            docRef.toObject(UserProfile::class.java)
        } else {
            null
        }
    }

    suspend fun saveProfile(profile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        // Use email as the document ID for simplicity and to enforce uniqueness (upsert).
        db.collection("profiles").document(profile.email).set(profile).get()
        return@withContext profile
    }
}