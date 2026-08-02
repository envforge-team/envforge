FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S envforge && adduser -S envforge -G envforge

COPY --from=build /app/target/*.jar app.jar

USER envforge
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
