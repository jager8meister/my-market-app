FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder --chown=appuser:appgroup /app/target/my-market-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
USER appuser
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/my_market_db \
    SPRING_DATASOURCE_USERNAME=my_market_user \
    SPRING_DATASOURCE_PASSWORD=my_market_password
ENTRYPOINT ["java", "-jar", "app.jar"]

