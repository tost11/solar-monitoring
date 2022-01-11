#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
WORKDIR backend
COPY backend/pom.xml pom.xml
RUN mvn dependency:go-offline
COPY backend/src src
RUN mvn -Dmaven.test.skip clean package

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build backend/target/solarmonitoring-0.0.1-SNAPSHOT.jar /usr/local/lib/solarmonitoring.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/solarmonitoring.jar"]