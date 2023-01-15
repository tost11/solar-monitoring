#
# Builds stage
#
FROM node:16.14.2-bullseye as frontend
COPY frontend/package.json app/frontend/package.json
WORKDIR /app/frontend
RUN npm install
COPY frontend /tmp/f
RUN cp -r /tmp/f/* /app/frontend
COPY version /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/\"version\": \"0.0.0\"/{s/\"version\": \"0.0.0\"/\"version\": \"$rep\"/g}" /app/frontend/package.json
RUN npm run build

#
# Build stage
#
FROM eclipse-temurin:17-focal AS build

RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz -P /tmp
RUN tar xf /tmp/apache-maven-*.tar.gz -C /opt
RUN ln -s /opt/apache-maven-3.8.7 /opt/maven

ENV JAVA_HOME=/opt/java/openjdk
ENV M2_HOME=/opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}

WORKDIR /app
COPY backend/pom.xml /app/pom.xml
RUN mvn dependency:go-offline
COPY backend/src /app/src
RUN mvn compile
COPY version /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/<version>0.0.0-SNAPSHOT<\/version>/{s/<version>0.0.0-SNAPSHOT<\/version>/<version>$rep-SNAPSHOT<\/version>/g}" /app/pom.xml
RUN mvn -Dmaven.test.skip clean package

#
# Package stage
#
FROM eclipse-temurin:17-jre-focal
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/target/solarmonitoring.jar /app/solarmonitoring.jar
COPY --from=frontend /app/frontend/dist /app/static

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring.jar"]