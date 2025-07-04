plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.8"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.coachassist"
version = "1.0-SNAPSHOT"

application {
    // Make sure this points to your Ktor application's entry point
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core-jvm:2.3.8")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.8")

    // Content negotiation and JSON serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.8")

    // Status pages for error handling
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}

tasks.shadowJar {
    archiveBaseName.set("coach-assist-backend")
    archiveClassifier.set("")
    archiveFileName.set("coach-assist-backend-all.jar")
    manifest {
        attributes("Main-Class" to "io.ktor.server.netty.EngineMain")
    }
}