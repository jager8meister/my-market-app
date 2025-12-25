FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy parent pom and all module poms (Maven needs to see all declared modules)
COPY pom.xml .
COPY api-contracts/pom.xml ./api-contracts/
COPY payment-service/pom.xml ./payment-service/
COPY market-app/pom.xml ./market-app/

# Copy api-contracts sources (needed for OpenAPI spec)
COPY api-contracts/src ./api-contracts/src

# Copy payment-service sources
COPY payment-service/src ./payment-service/src

# Build only payment-service module
RUN mvn -B -pl payment-service -am package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder --chown=appuser:appgroup /build/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
USER appuser
ENV SPRING_R2DBC_URL=r2dbc:postgresql://payment-db:5432/payment_db \
    SPRING_R2DBC_USERNAME=payment_user \
    SPRING_R2DBC_PASSWORD=payment_password
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
