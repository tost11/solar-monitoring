FROM eclipse-temurin:17-jdk-noble AS build

RUN apt update && apt install -y maven

ENV JAVA_HOME=/opt/java/openjdk

COPY pom.xml /pom.xml
COPY lib/pom.xml /lib/pom.xml
COPY testlib/pom.xml /testlib/pom.xml
COPY updater/pom.xml /updater/pom.xml
RUN mvn dependency:go-offline -P updater

COPY versions/updater /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/<version>0.0.0-SNAPSHOT<\/version>/{s/<version>0.0.0-SNAPSHOT<\/version>/<version>$rep-SNAPSHOT<\/version>/g}" /updater/pom.xml

COPY lib/src /lib/src
COPY testlib/src /testlib/src
COPY updater/src /updater/src
RUN mvn -Dmaven.test.skip package -P updater

#
# Package stage
#
FROM eclipse-temurin:17-jre-noble
EXPOSE 8080
EXPOSE 8060
WORKDIR /app
COPY --from=build /updater/target/solarmonitoring-updater.jar /app/solarmonitoring-updater.jar

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring-updater.jar"]