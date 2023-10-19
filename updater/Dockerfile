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
COPY updater/pom.xml /updater/pom.xml
RUN mvn dependency:go-offline -P updater

COPY versions/updater /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/<version>0.0.0-SNAPSHOT<\/version>/{s/<version>0.0.0-SNAPSHOT<\/version>/<version>$rep-SNAPSHOT<\/version>/g}" /updater/pom.xml

COPY lib/src /lib/src
COPY updater/src /updater/src
RUN mvn -Dmaven.test.skip package -P updater

#
# Package stage
#
FROM eclipse-temurin:17-jre-focal
EXPOSE 8080
EXPOSE 8060
WORKDIR /app
COPY --from=build /updater/target/solarmonitoring-updater.jar /app/solarmonitoring-updater.jar

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring-updater.jar"]