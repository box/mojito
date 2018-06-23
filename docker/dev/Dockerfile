FROM maven:3.5.4-jdk-8
RUN apt-get update && apt-get install -y build-essential python nodejs-legacy libpng-dev pngquant
VOLUME ["/tmp"]
WORKDIR /mnt/mojito
ENV PATH="/mnt/mojito/webapp/node/:${PATH}"

