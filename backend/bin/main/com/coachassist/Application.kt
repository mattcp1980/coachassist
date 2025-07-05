package com.coachassist

import com.coachassist.config.FirestoreConfig
import com.coachassist.plugins.configureHTTP
import com.coachassist.plugins.configureRouting
import com.coachassist.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    // Set gRPC system properties BEFORE anything else
    System.setProperty("io.grpc.internal.DnsNameResolverProvider.enable_grpclb", "false")
    System.setProperty("io.grpc.internal.DnsNameResolverProvider.enable_service_config", "false")
    
    // Now start the Ktor server
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Initialize Firestore at startup — optional but recommended
    FirestoreConfig.initialize()

    configureSerialization()
    configureHTTP()
    configureRouting()
}