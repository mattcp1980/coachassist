package com.coachassist.plugins

import com.coachassist.models.SessionPlanRequest
import com.coachassist.models.UserProfile
import com.coachassist.services.SessionPlannerService
import com.coachassist.services.UserProfileService
import com.google.cloud.firestore.FirestoreOptions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred."))
                }
            }

            route("/profiles") {
                post {
                    try {
                        val profile = call.receive<UserProfile>()
                        val savedProfile = userProfileService.saveProfile(profile)
                        call.respond(HttpStatusCode.OK, savedProfile)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred while saving the profile."))
                    }
                }

                get {
                    val email = call.request.queryParameters["email"]
                    if (email.isNullOrBlank()) {
                        return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email query parameter is required."))
                    }
                    val profile = userProfileService.getProfileByEmail(email)
                    profile?.let { call.respond(HttpStatusCode.OK, it) } ?: call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}