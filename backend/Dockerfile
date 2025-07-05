# Use Debian-based Java 17 Runtime
FROM eclipse-temurin:17-jre

# Install CA certificates for gRPC HTTPS
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the pre-built fat JAR
COPY build/libs/coach-assist-backend-all.jar app.jar

# (Optional) Copy service account key if using static file
# COPY firebase-service-account.json /app/firebase-service-account.json

# (Optional) Set env var if copying service account
# ENV GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-service-account.json

EXPOSE 8080

# JVM arguments for container support and gRPC/netty compatibility
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Dio.netty.transport.noNative=true", \
     "-Dio.grpc.netty.useCustomAllocator=false", \
     "-Dio.netty.noUnsafe=true", \
     "-Dio.netty.transport.noEpoll=true", \
     "-Dio.netty.transport.noKqueue=true", \
     "-Djava.awt.headless=true", \
     "-server", \
     "-jar", "app.jar"]
