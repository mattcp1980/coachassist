plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.8"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.google.cloud.tools.jib") version "3.4.1"
    application
}

group = "com.coachassist"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:2.3.8")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.8")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.8")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.8")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.8")

    // Google GenAI
    implementation("com.google.genai:google-genai:0.6.1")

    // Google Cloud
    implementation("com.google.cloud:google-cloud-firestore:3.15.0")

    // gRPC dependencies — pin to 1.61.1 to keep InternalGlobalInterceptors
    implementation("io.grpc:grpc-core:1.61.1")
    implementation("io.grpc:grpc-api:1.61.1")
    implementation("io.grpc:grpc-context:1.61.1")
    implementation("io.grpc:grpc-netty-shaded:1.61.1")
    implementation("io.grpc:grpc-protobuf:1.61.1")
    implementation("io.grpc:grpc-stub:1.61.1")
    implementation("com.google.api:gax-grpc:2.49.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}

// ✅ Jib configuration — packaged mode
jib {
    containerizingMode = "packaged"
    from {
        image = "eclipse-temurin:17-jre"
    }
    to {
        image = "europe-west1-docker.pkg.dev/amiable-wonder-261523/coach-assist-repo/coach-assist-service"
        tags = setOf("latest")
    }
    container {
        mainClass = "io.ktor.server.netty.EngineMain"
        ports = listOf("8080")
        jvmFlags = listOf(
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0",
            "-Dio.netty.transport.noNative=true",
            "-Dio.grpc.netty.useCustomAllocator=false",
            "-Dio.netty.noUnsafe=true",
            "-Dio.netty.transport.noEpoll=true",
            "-Dio.netty.transport.noKqueue=true",
            "-Djava.awt.headless=true",
            "-server"
        )
    }
}
