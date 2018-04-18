FROM java:8-jre

ARG PACKAGE_VERSION=0.2.1-SNAPSHOT

EXPOSE 80
COPY "service/build/flippy-standalone-$PACKAGE_VERSION.jar" "/usr/src/flippy-standalone.jar"
ENTRYPOINT [ "java", "-jar", "/usr/src/flippy-standalone.jar" ]
