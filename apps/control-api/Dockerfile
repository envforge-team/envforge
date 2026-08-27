FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY apps/control-api/.mvn/ .mvn/
COPY apps/control-api/mvnw apps/control-api/pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY apps/control-api/src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S envforge && adduser -S envforge -G envforge

COPY --from=build /app/target/*.jar app.jar

# Helm chart used for provisioning ephemeral environments
COPY deployment/helm/envforge-workload /app/deployment/helm/envforge-workload

USER envforge
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]