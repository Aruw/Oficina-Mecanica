# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk upgrade --no-cache && addgroup -S oficina && adduser -S oficina -G oficina
COPY --from=build /build/target/oficina-*.jar app.jar
USER oficina

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
