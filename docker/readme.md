# Docker compose

Run `docker-compose up` from within `mojito/docker/` or `docker-compose -f docker/docker-compose.yml up` from the 
project directory. It will start Mysql and build/start the Webapp.

To re-use a pre-built image, uncomment the `image` configuration in `docker-compose.yml`.

# Find IP on Mac

If you running on Mac, `localhost` may not work. To get the IP to reach the service: `docker-machine ip default`.

`export l10n_resttemplate_host="192.168.99.111" ` and then whatever CLI command you have to ru, eg. 
`mojito demo-create -n dockertest`

# Multi-stage build

To build manually `docker build -t aurambaj/mojito-webapp-ms8 -f docker/Dockerfile-ms8 .`

# Multi-stage build with Buildkit 

`DOCKER_BUILDKIT=1 docker build -t aurambaj/mojito-webapp-bk8 -f docker/Dockerfile-bk8 .`

# Old, create image from already built jars

First build the Webapp with `mvn clean install -DskipTests`

In the `webapp/target` directory run `docker build -t aurambaj/mojito-webapp-old -f ../src/main/docker/Dockerfile .`

To test run it: `docker run -p 8080:8080 aurambaj/mojito-webapp-old:latest`

# Local builds using docker compose

Create a docker image only with the build binaries, and use docker-compose to build locally by mounting the source code
and `.m2` repository

`docker-compose -f docker/docker-compose-build.yml run compile`

# Common issues / Random notes

Incompatibility Mac/Linux for `node/` `node_modules/`, remove the directories before calling docker commands. Some 
`Dockerfile` remove the directories explicitly.

