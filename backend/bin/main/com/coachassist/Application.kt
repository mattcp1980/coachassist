package com.coachassist

import com.coachassist.plugins.configureRouting
import com.coachassist.plugins.configureSerialization
import io.ktor.server.application.*

// The main function is now delegated to Ktor's EngineMain,
// which reads configuration from application.conf
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureRouting()
}