# blog-server Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY target/*.jar app.jar

# Application configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
