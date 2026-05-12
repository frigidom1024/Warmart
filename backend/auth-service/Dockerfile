FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY auth-service/pom.xml auth-service/
RUN mvn dependency:go-offline -pl auth-service -am -q 2>/dev/null || true
COPY . .
RUN mvn clean package -pl auth-service -am -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
