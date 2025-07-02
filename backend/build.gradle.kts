plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.coachassist"
version = "0.0.1"

application {
    mainClass.set("com.coachassist.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Ktor Content Negotiation for JSON
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)

    // Ktor Status Pages for error handling
    implementation(libs.ktor.server.statusPages)

    // Logging
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> { // This is often redundant if the application plugin is configured, but it's harmless.
    manifest {
        attributes["Main-Class"] = "com.coachassist.ApplicationKt"
    }
}