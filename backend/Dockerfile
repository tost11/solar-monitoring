#
# Builds stage
#
FROM node:16.14.2-bullseye AS frontend
COPY frontend/package.json app/frontend/package.json
COPY frontend/package-lock.json app/frontend/package-lock.json
COPY frontend/.npmrc app/frontend/.npmrc
WORKDIR /app/frontend
RUN npm ci --prefer-offline
COPY frontend /tmp/f
RUN cp -rT /tmp/f /app/frontend
COPY versions/app /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/\"version\": \"0.0.0\"/{s/\"version\": \"0.0.0\"/\"version\": \"$rep\"/g}" /app/frontend/package.json
RUN npm run build

#
# Build stage
#
FROM eclipse-temurin:17-focal AS build

RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz -P /tmp
RUN tar xf /tmp/apache-maven-*.tar.gz -C /opt
RUN ln -s /opt/apache-maven-3.8.8 /opt/maven

ENV JAVA_HOME=/opt/java/openjdk
ENV M2_HOME=/opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}

COPY pom.xml /pom.xml
COPY lib/pom.xml /lib/pom.xml
COPY testlib/pom.xml /testlib/pom.xml
COPY backend/pom.xml /backend/pom.xml
RUN mvn dependency:go-offline -P app

COPY versions/app /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/<version>0.0.0-SNAPSHOT<\/version>/{s/<version>0.0.0-SNAPSHOT<\/version>/<version>$rep-SNAPSHOT<\/version>/g}" /backend/pom.xml

COPY lib/src /lib/src
COPY testlib/src /testlib/src
COPY backend/src /backend/src
RUN mvn -Dmaven.test.skip package -P app

#
# Package stage
#
FROM eclipse-temurin:17-jre-focal
EXPOSE 8080
EXPOSE 8060
WORKDIR /app
COPY --from=build /backend/target/solarmonitoring-app.jar /app/solarmonitoring-app.jar
COPY --from=frontend /app/frontend/dist /app/static

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring-app.jar"]