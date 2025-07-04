# Use a slim, alpine-based Java 17 Runtime for a smaller final image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the pre-built fat JAR from the build context.
# The deploy script builds this JAR before running docker build.
COPY backend/build/libs/coach-assist-backend-all.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]