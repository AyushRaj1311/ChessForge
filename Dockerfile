# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Railway sets the PORT environment variable automatically
ENV PORT=1111
EXPOSE ${PORT}
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
