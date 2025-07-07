package com.coachassist.services

import com.coachassist.models.SessionPlan
import com.coachassist.models.SessionPlanRequest
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            
            Additional Context for the request "${request.extra}"

            The plan should be broken down based on the desired time breakdown provided as percentages of the total duration provided.
            For each part, provide a brief overview of the activity, its duration, required equipment and the key coaching points. 
            
            Return the response in detailed markdown format with:
            - Clear section headers (## Section Name)
            - Activity lists with bullet points
            - Duration and timing information clearly marked
            - Equipment lists
            - Coaching notes in separate sections
            This will make it easy to parse and display in a web interface.
        """.trimIndent()
        
        // Send the prompt to the AI and get the response.
        // Use withContext to run the blocking call on IO dispatcher
        val response: GenerateContentResponse = withContext(Dispatchers.IO) {
            client.models.generateContent("gemini-1.5-flash", prompt, null)
        }
        
        return SessionPlan(plan = response.text() ?: "Could not generate a plan.")
    }
}