package com.coachassist.services

import com.coachassist.models.SessionPlanRequest
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A simple data class to represent the AI's response.
@Serializable
data class SessionPlan(val plan: String)

class SessionPlannerService(apiKey: String) {
    private val client = Client.builder()
        .apiKey(apiKey)
        .build()
    
    suspend fun generatePlan(request: SessionPlanRequest): SessionPlan {
        if (request.ageGroup.isBlank()) {
            throw IllegalArgumentException("Age group cannot be empty.")
        }
        if (request.focus.isBlank()) {
            throw IllegalArgumentException("Focus cannot be empty.")
        }
        if (request.durationMinutes <= 0) {
            throw IllegalArgumentException("Duration must be a positive number.")
        }
        if (request.numberOfPlayers <= 0) {
            throw IllegalArgumentException("Number of players must be a positive number.")
        }
        
        // Create a detailed prompt for the AI model.
        val prompt = """
            You are a sports coach assistant for a "${request.ageGroup}" football team.
            Create a summary coaching session plan based on the following criteria:
            - Main Focus: "${request.focus}"
            - Total Duration: ${request.durationMinutes} minutes
            - Number of Players: ${request.numberOfPlayers}
            - Desired Time Breakdown: "${request.timeBreakdown}"
            
            The plan should be broken down based on the desired time breakdown provided as percentages of the total duration provided.
            For each part, provide a brief overview of the activity, its duration, required e
            quipment and the key coaching points. 
            
            Return the response as a single block of text.
        """.trimIndent()
        
        // Send the prompt to the AI and get the response.
        // Use withContext to run the blocking call on IO dispatcher
        val response: GenerateContentResponse = withContext(Dispatchers.IO) {
            client.models.generateContent("gemini-1.5-flash", prompt, null)
        }
        
        return SessionPlan(plan = response.text() ?: "Could not generate a plan.")
    }
}