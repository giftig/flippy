FROM java:8-jre

EXPOSE 80
COPY "build/flippy-standalone-0.2.1-SNAPSHOT.jar" "/usr/src/flippy-standalone.jar"
ENTRYPOINT [ "java", "-jar", "/usr/src/flippy-standalone.jar" ]
