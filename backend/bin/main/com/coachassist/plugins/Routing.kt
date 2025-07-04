package com.coachassist.plugins

import com.coachassist.models.SessionRequest
import com.coachassist.services.SessionPlannerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val sessionPlannerService = SessionPlannerService()

    routing {
        route("/api/v1") {
            post("/session-plan") {
                try {
                    val request = call.receive<SessionRequest>()
                    val sessionPlan = sessionPlannerService.generatePlan(request)
                    call.respond(HttpStatusCode.OK, sessionPlan)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred."))
                }
            }
        }
    }
}