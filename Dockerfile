
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app

COPY --from=build /app/target/app.jar ./app.jar

COPY challenge.db ./challenge.db

EXPOSE 4567
CMD ["java", "-jar", "app.jar"]
