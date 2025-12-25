FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY api-contracts/pom.xml ./api-contracts/
COPY market-app/pom.xml ./market-app/
COPY payment-service/pom.xml ./payment-service/

COPY api-contracts/src ./api-contracts/src

COPY market-app/src ./market-app/src

RUN mvn -B -pl market-app -am package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder --chown=appuser:appgroup /build/market-app/target/market-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
USER appuser
ENV SPRING_R2DBC_URL=r2dbc:postgresql://db:5432/my_market_db \
    SPRING_R2DBC_USERNAME=my_market_user \
    SPRING_R2DBC_PASSWORD=my_market_password \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    PAYMENT_SERVICE_URL=http://payment-service:8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

