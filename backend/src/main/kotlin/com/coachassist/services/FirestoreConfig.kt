package com.coachassist.config

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

object FirestoreConfig {

    @Volatile
    private var firestore: Firestore? = null

    fun initialize(): Firestore {
        if (firestore == null) {
            synchronized(this) {
                if (firestore == null) {
                    try {
                        val projectId = System.getenv("GOOGLE_CLOUD_PROJECT")
                            ?: throw IllegalStateException("GOOGLE_CLOUD_PROJECT environment variable is not set")

                        // Use Application Default Credentials for Cloud Run
                        val credentials = try {
                            // Try to load from file first (for local development)
                            val credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
                            if (credentialsPath != null) {
                                println("🔑 Loading credentials from file: $credentialsPath")
                                FileInputStream(credentialsPath).use {
                                    GoogleCredentials.fromStream(it)
                                }
                            } else {
                                // Use Application Default Credentials (for Cloud Run)
                                println("🔑 Using Application Default Credentials")
                                GoogleCredentials.getApplicationDefault()
                            }
                        } catch (e: Exception) {
                            println("🔑 Falling back to Application Default Credentials")
                            GoogleCredentials.getApplicationDefault()
                        }

                        // Simplified Firestore initialization - let it use default gRPC settings
                        val options = FirestoreOptions.newBuilder()
                            .setProjectId(projectId)
                            .setCredentials(credentials)
                            .build()

                        firestore = options.service

                        // Test connection with timeout
                        firestore?.let { fs ->
                            println("🔄 Testing Firestore connection...")
                            fs.collection("_connection_test").limit(1).get().get(10, TimeUnit.SECONDS)
                        }
                        println("✅ Firestore initialized successfully for project: $projectId")

                    } catch (e: Exception) {
                        println("❌ Failed to initialize Firestore: ${e.message}")
                        println("❌ Stack trace: ${e.stackTrace.joinToString("\n")}")
                        // Reset firestore to null on failure so retry is possible
                        firestore = null
                        throw e
                    }
                }
            }
        }
        return firestore!!
    }

    fun getInstance(): Firestore {
        return firestore ?: initialize()
    }

    suspend fun <T> withFirestore(block: suspend (Firestore) -> T): T {
        return withContext(Dispatchers.IO) {
            block(getInstance())
        }
    }

    // Additional utility methods
    fun isInitialized(): Boolean = firestore != null

    fun shutdown() {
        firestore?.let { fs ->
            try {
                fs.close()
                println("✅ Firestore connection closed")
            } catch (e: Exception) {
                println("⚠️ Error closing Firestore: ${e.message}")
            } finally {
                firestore = null
            }
        }
    }

    // For testing - allows injection of mock
    internal fun setFirestoreForTesting(mockFirestore: Firestore) {
        firestore = mockFirestore
    }

    // Reset for testing
    internal fun resetForTesting() {
        firestore = null
    }
}