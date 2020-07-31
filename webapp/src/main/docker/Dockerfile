FROM openjdk:8-alpine
VOLUME /tmp
ADD mojito-webapp-*-exec.jar app.jar
RUN sh -c 'touch /app.jar'
# starting with "exec doesn't seem to be needed with openjdk:8-alpine. As per docker documentation, it is required in general
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar
