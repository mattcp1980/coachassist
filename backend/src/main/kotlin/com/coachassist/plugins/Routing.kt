package com.coachassist.plugins

import com.coachassist.models.SessionPlanRequest
import com.coachassist.models.UserProfile
import com.coachassist.services.SessionPlannerService
import com.coachassist.services.UserProfileService
import com.google.cloud.firestore.FirestoreOptions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.serialization.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException

fun Application.configureRouting() {
    // Read external configuration once and inject it into services.
    val apiKey = System.getenv("GEMINI_API_KEY")
        ?: throw IllegalStateException("GEMINI_API_KEY environment variable not set.")

    // Initialize service clients and dependencies.
    val firestoreDb = FirestoreOptions.getDefaultInstance().service
    val sessionPlannerService = SessionPlannerService(apiKey)
    val userProfileService = UserProfileService(firestoreDb)

    routing {
        
        get("/health") {
            call.respondText("OK", status = HttpStatusCode.OK)
        }
        
        route("/api/v1") {
            post("/session-plan") {
                try {
                    val request = call.receive<SessionPlanRequest>()
                    val sessionPlan = sessionPlannerService.generatePlan(request)
                    call.respond(HttpStatusCode.OK, sessionPlan)
                } catch (e: SerializationException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body: ${e.cause?.message}"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    application.log.error("Failed to generate session plan", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred while generating the session plan."))
                }
            }

            route("/profiles") {
                post {
                    try {
                        val profile = call.receive<UserProfile>()
                        val savedProfile = userProfileService.saveProfile(profile)
                        call.respond(HttpStatusCode.Created, savedProfile)
                    } catch (e: SerializationException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body: ${e.cause?.message}"))
                    } catch (e: Exception) {
                        application.log.error("Failed to save profile", e)
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred while saving the profile."))
                    }
                }

                get {
                    val email = call.request.queryParameters["email"]
                    if (email.isNullOrBlank()) {
                        return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email must be provided as query parameter."))
                    }
                    
                    try {
                        val profile = userProfileService.getProfileByEmail(email)
                        profile?.let { call.respond(HttpStatusCode.OK, it) } ?: call.respond(HttpStatusCode.NotFound)
                    } catch (e: Exception) {
                        application.log.error("Failed to get profile for email $email", e)
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An error occurred while fetching the profile. Check server logs for details."))
                    }
                }
            }
        }
    }
}