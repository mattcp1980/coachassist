import java.io.ByteArrayOutputStream
import java.time.Instant

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

    // Google Cloud Firestore
    implementation("com.google.cloud:google-cloud-firestore:3.15.0")

    // gRPC versions pinned to avoid "InternalGlobalInterceptors" error
    implementation("io.grpc:grpc-core:1.61.1")
    implementation("io.grpc:grpc-api:1.61.1")
    implementation("io.grpc:grpc-context:1.61.1")
    implementation("io.grpc:grpc-netty-shaded:1.61.1")
    implementation("io.grpc:grpc-protobuf:1.61.1")
    implementation("io.grpc:grpc-stub:1.61.1")
    implementation("com.google.api:gax-grpc:2.49.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Tests
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}

configurations.all {
    resolutionStrategy {
        force(
            "io.grpc:grpc-core:1.61.1",
            "io.grpc:grpc-api:1.61.1",
            "io.grpc:grpc-context:1.61.1",
            "io.grpc:grpc-netty-shaded:1.61.1",
            "io.grpc:grpc-protobuf:1.61.1",
            "io.grpc:grpc-stub:1.61.1"
        )
    }
}

// Helper function to get Git hash for reproducible builds
fun getGitHash(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// Helper function to get current timestamp
fun getTimestamp(): String {
    return System.currentTimeMillis().toString()
}

// Helper function to get build tag (priority: ENV var > Git hash > timestamp)
fun getBuildTag(): String {
    val envTag = System.getenv("BUILD_TAG")
    if (!envTag.isNullOrBlank()) {
        return envTag
    }
    
    val gitHash = getGitHash()
    if (gitHash != "unknown") {
        return gitHash
    }
    
    return getTimestamp()
}

jib {
    containerizingMode = "packaged"
    
    from {
        image = "eclipse-temurin:17-jre"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
        }
    }
    
    to {
        image = "europe-west1-docker.pkg.dev/amiable-wonder-261523/coach-assist-repo/coach-assist-service"
        // Always include latest + a unique tag for proper versioning
        tags = setOf("latest", getBuildTag())
    }
    
    container {
        mainClass = "io.ktor.server.netty.EngineMain"
        ports = listOf("8080")
        
        // Optimized JVM flags for containerized Ktor application
        jvmFlags = listOf(
            // Container support
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0",
            "-XX:InitialRAMPercentage=50.0",
            
            // Network optimizations
            "-Dio.netty.transport.noNative=true",
            "-Dio.grpc.netty.useCustomAllocator=false",
            "-Dio.netty.noUnsafe=true",
            "-Dio.netty.transport.noEpoll=true",
            "-Dio.netty.transport.noKqueue=true",
            
            // General optimizations
            "-Djava.awt.headless=true",
            "-Dfile.encoding=UTF-8",
            "-Duser.timezone=UTC",
            "-server",
            
            // GC optimizations for Cloud Run
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:MaxGCPauseMillis=100"
        )
        
        // Environment variables
        environment = mapOf(
            "JAVA_TOOL_OPTIONS" to "-Dfile.encoding=UTF-8",
            "TZ" to "UTC"
        )
        
        // Labels for better container management
        labels.set(mapOf(
            "maintainer" to "coach-assist-team",
            "version" to version.toString(),
            "build-time" to Instant.now().toString(),
            "git-hash" to getGitHash()
        ))
        
        // Resource hints
        creationTime = "USE_CURRENT_TIMESTAMP"
        
        // User configuration (non-root for security)
        user = "1000:1000"
        workingDirectory = "/app"
        
        // Health check configuration
        ports = listOf("8080")
        volumes = listOf("/tmp")
    }
    
    // Docker build configuration
    dockerClient {
        executable = "docker"
    }
}

// Custom tasks for different deployment scenarios
tasks.register("jibBuildWithTimestamp") {
    group = "deployment"
    description = "Build and push container with timestamp tag"
    
    doFirst {
        val timestamp = getTimestamp()
        println("Building with timestamp tag: $timestamp")
        System.setProperty("jib.to.tags", "latest,$timestamp")
    }
    
    finalizedBy("jib")
}

tasks.register("jibBuildWithGitHash") {
    group = "deployment"
    description = "Build and push container with git hash tag"
    
    doFirst {
        val gitHash = getGitHash()
        println("Building with git hash tag: $gitHash")
        System.setProperty("jib.to.tags", "latest,$gitHash")
    }
    
    finalizedBy("jib")
}

tasks.register("printImageInfo") {
    group = "deployment"
    description = "Print container image information"
    
    doLast {
        println("Container Image Information:")
        println("  Registry: europe-west1-docker.pkg.dev")
        println("  Project: amiable-wonder-261523")
        println("  Repository: coach-assist-repo")
        println("  Service: coach-assist-service")
        println("  Tags: latest, ${getBuildTag()}")
        println("  Full Image: europe-west1-docker.pkg.dev/amiable-wonder-261523/coach-assist-repo/coach-assist-service:${getBuildTag()}")
    }
}

// Ensure clean builds
tasks.named("jib") {
    dependsOn("build")
}

tasks.named("jibDockerBuild") {
    dependsOn("build")
}