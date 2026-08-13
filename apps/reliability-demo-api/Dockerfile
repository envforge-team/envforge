# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: runtime
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --uid 10001 --shell /bin/false --no-create-home appuser

COPY --from=build /app/target/reliability-demo-api.jar app.jar
RUN chown appuser:appuser app.jar

USER 10001

ARG APP_VERSION=0.1.0
ENV APP_VERSION=${APP_VERSION}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
