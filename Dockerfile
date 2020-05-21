FROM gradle:6.4.1-jdk8 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle fatjar --no-daemon 

FROM ubuntu:20.04
RUN apt-get update && apt-get install openjdk-8-jre-headless -y
ADD dune-2020.01.0.tar.bz2 /usr/local
COPY start.sh /usr/local/start.sh
COPY --from=build /home/gradle/src/build/libs/imcserver-1.0-SNAPSHOT.jar /usr/local/ImcServer.jar
CMD ["/usr/bin/bash","/usr/local/start.sh"]

EXPOSE 8009/tcp

