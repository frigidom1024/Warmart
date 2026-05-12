FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl gateway -am -q 2>/dev/null || true
COPY . .
RUN mvn clean package -pl gateway -am -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
