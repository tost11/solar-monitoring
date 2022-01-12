#
# Builds stage
#
FROM node:16.3 AS node
COPY frontend frontend
WORKDIR frontend
RUN apt install libm6
RUN npm install
RUN npm run build

#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
WORKDIR backend
COPY backend/pom.xml pom.xml
RUN mvn dependency:go-offline
COPY backend/src src
COPY --from=node frontend/dist src/main/resources/public
RUN mvn -Dmaven.test.skip clean package

#
# Package stage
#
FROM openjdk:11-jre-slim
RUN mkdir /app
COPY --from=build backend/target/solarmonitoring-0.0.1-SNAPSHOT.jar /app/solarmonitoring.jar
COPY backend/src/main/resources/solar-template-selfmade-device.json /app/solar-template-selfmade-device.json
EXPOSE 8080
WORKDIR /app
ENTRYPOINT ["java","-jar","solarmonitoring.jar"]