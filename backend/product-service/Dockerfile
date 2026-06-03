FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY product-service/pom.xml product-service/
RUN mvn dependency:go-offline -pl product-service -am -q 2>/dev/null || true
COPY . .
RUN mvn clean package -pl product-service -am -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/product-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
