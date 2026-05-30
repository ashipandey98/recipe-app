FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml /app/
COPY src /app/src/
RUN mvn -f /app/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
