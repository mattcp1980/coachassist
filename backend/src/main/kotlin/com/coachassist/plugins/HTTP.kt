package com.coachassist.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    install(CORS) {

        allowHost("lovable.dev", schemes = listOf("https")) // For the builder
        allowHost("coachassist.lovable.app", schemes = listOf("https")) // For the deployed app

        // Allow common HTTP methods and headers
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization) // Good to have for future auth needs
    }
}