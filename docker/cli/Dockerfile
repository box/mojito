FROM openjdk:8

ENV MOJITO_BIN=/usr/local/mojito/bin
ENV PATH $PATH:${MOJITO_BIN}
ENV MOJITO_VERSION=0.109

# Download Mojito CLI jar
RUN mkdir -p ${MOJITO_BIN} && \
    curl -sSL https://github.com/box/mojito/releases/download/v${MOJITO_VERSION}/mojito-cli-${MOJITO_VERSION}.jar \
         -o ${MOJITO_BIN}/mojito-cli-${MOJITO_VERSION}.jar

# Create the shell wrapper for the jar
RUN echo "#!/bin/bash \n\
java -Dl10n.resttemplate.host=localhost \\\\\n \
     -Dl10n.resttemplate.scheme=http \\\\\n \
     -Dl10n.resttemplate.port=8080 \\\\\n \
     -jar $MOJITO_BIN/mojito-cli-${MOJITO_VERSION}.jar \$@" \
    >> /usr/local/mojito/bin/mojito && chmod +x $MOJITO_BIN/mojito
