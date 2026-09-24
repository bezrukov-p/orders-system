FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# -------------------------------------------------------------------------
# 2. Качаем зависимости order-service + его модульных зависимостей (common).
#    -pl order-service — только order-service
#    -am — "also make" — собрать common, если order-service от него зависит
# -------------------------------------------------------------------------
RUN mvn -B -pl order-service -am dependency:go-offline

# -------------------------------------------------------------------------
# 3. Копируем исходники order-service И common.
#    common нужен, потому что order-service от него зависит — без него
#    не скомпилируется.
# -------------------------------------------------------------------------
COPY common/src common/src
COPY order-service/src order-service/src

# -------------------------------------------------------------------------
# 4. Собираем. -am автоматически соберёт common первым.
# -------------------------------------------------------------------------
RUN mvn -B -pl order-service -am clean package -DskipTests

# =========================================================================
# Stage 2: runtime
# =========================================================================
FROM eclipse-temurin:25-jre-alpine

ARG OTEL_AGENT_VERSION=2.31.1

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /build/order-service/target/*.jar app.jar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /app/otel-agent.jar
RUN chown app:app /app/otel-agent.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/otel-agent.jar -jar app.jar"]