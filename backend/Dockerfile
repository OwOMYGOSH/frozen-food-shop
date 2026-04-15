# Dockerfile — 正式機用（JVM 最佳化版本）
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /work
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S frozen && adduser -S frozen -G frozen
COPY --from=builder /work/target/quarkus-app/lib/ ./lib/
COPY --from=builder /work/target/quarkus-app/*.jar ./
COPY --from=builder /work/target/quarkus-app/app/ ./app/
COPY --from=builder /work/target/quarkus-app/quarkus/ ./quarkus/
USER frozen
EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
