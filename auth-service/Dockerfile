FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add curl for healthcheck
RUN apk add --no-cache curl

# Create a non-root user to run the application
RUN addgroup -S swiftchat && adduser -S swiftchat -G swiftchat

# Copy the JAR file
COPY target/*.jar app.jar

# Change ownership of the application files
RUN chown -R swiftchat:swiftchat /app

# Switch to the non-root user
USER swiftchat

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

# JVM configuration for containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose port
EXPOSE 8081

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1
