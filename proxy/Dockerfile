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
FROM eclipse-temurin:17-jre-focal
EXPOSE 8080
EXPOSE 8060
WORKDIR /app
COPY --from=build /proxy/target/solarmonitoring-proxy.jar /app/solarmonitoring-proxy.jar

RUN useradd -ms /bin/bash runuser
WORKDIR /app

RUN chown -R runuser:runuser /app

USER runuser

CMD ["java","-jar","solarmonitoring-proxy.jar"]