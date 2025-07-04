plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    application
}

application {
    // Set the main class to the entry point of the Pulumi program
    mainClass.set("com.coachassist.infra.CoachAssistStackKt")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.pulumi:pulumi:0.16.1")
    implementation("com.pulumi:gcp:8.8.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

tasks.jar {
    archiveBaseName.set("infrastructure")
    archiveVersion.set("")
    archiveClassifier.set("")
}