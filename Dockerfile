#
# Builds stage
#
FROM node:16-alpine3.14 as frontend
COPY frontend app/frontend
WORKDIR /app/frontend
RUN npm install
RUN npm run build

#
# Build stage
#
FROM adoptopenjdk:11-jdk-hotspot AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY backend/pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY backend/src /app/src
COPY --from=frontend /app/frontend/dist /app/src/main/resources/public
RUN mvn -Dmaven.test.skip clean package

#
# Package stage
#
FROM adoptopenjdk:11-jre-hotspot
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/target/solarmonitoring.jar /app/solarmonitoring.jar
COPY backend/src/main/resources/solar-template-selfmade-device.json /app/solar-template-selfmade-device.json
ENTRYPOINT ["java","-jar","solarmonitoring.jar"]