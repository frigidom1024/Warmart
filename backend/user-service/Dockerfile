FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY user-service/pom.xml user-service/
RUN mvn dependency:go-offline -pl user-service -am -q 2>/dev/null || true
COPY . .
RUN mvn clean package -pl user-service -am -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
