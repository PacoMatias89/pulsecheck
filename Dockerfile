FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
RUN addgroup -S pulsecheck && adduser -S pulsecheck -G pulsecheck
COPY --from=builder /app/target/*.jar app.jar
RUN chown pulsecheck:pulsecheck app.jar
USER pulsecheck
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
