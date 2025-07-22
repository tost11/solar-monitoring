FROM eclipse-temurin:17-jdk-noble AS build

RUN apt update && apt install -y maven

ENV JAVA_HOME=/opt/java/openjdk

COPY pom.xml /pom.xml
COPY lib/pom.xml /lib/pom.xml
COPY testlib/pom.xml /testlib/pom.xml
COPY proxy/pom.xml /proxy/pom.xml
RUN mvn dependency:go-offline -P proxy

COPY versions/proxy /tmp/version
RUN rep=$(cat /tmp/version) && \
     echo "Version is: $rep" && \
     sed -i -e "0,/<version>0.0.0-SNAPSHOT<\/version>/{s/<version>0.0.0-SNAPSHOT<\/version>/<version>$rep-SNAPSHOT<\/version>/g}" /proxy/pom.xml

COPY lib/src /lib/src
COPY testlib/src /testlib/src
COPY proxy/src /proxy/src
RUN mvn -Dmaven.test.skip package -P proxy

#
# Package stage
#
FROM eclipse-temurin:17-jre-noble
EXPOSE 8080
EXPOSE 8060
WORKDIR /app
COPY --from=build /proxy/target/solarmonitoring-proxy.jar /app/solarmonitoring-proxy.jar

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring-proxy.jar"]