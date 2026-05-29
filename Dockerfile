FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/BE-1TEST-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 10000
CMD ["java", "-jar", "/app/app.jar"]
