FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY order-service/pom.xml order-service/
RUN mvn dependency:go-offline -pl order-service -am -q 2>/dev/null || true
COPY . .
RUN mvn clean package -pl order-service -am -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
