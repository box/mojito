# Docker compose

Run `docker-compose up` from within `mojito/docker/` or `docker-compose -f docker/docker-compose.yml up` from the
project directory. It will start Mysql and build/start the Webapp.

In detached mode `docker compose up -d`. And to remove everything including volumes: `docker compose rm -s -v` to remove
volumes.

To re-use a pre-built image, uncomment the `image` configuration in `docker-compose.yml`.

For older version, may need to set some env variable `COMPOSE_DOCKER_CLI_BUILD=1 DOCKER_BUILDKIT=1 docker-compose up`

## To use a local data directory

Create the data directory for mysql: `mkdir mojito/docker/.data/db`

```
services:
  db:
    image: mysql:5.7
    volumes:
      - "./.data/db:/var/lib/mysql"
```

or named volumes

```
volumes:
  - mojito_mysql
    
services:
  db:
    image: mysql:5.7
    volumes:
      - "mojito_mysql:/var/lib/mysql"
```

## Common issues

Incompatibility Mac/Linux for `node/` `node_modules/`, remove the directories before calling docker commands. Some
`Dockerfile` remove the directories explicitly.

# One-off build and push

## CLI image

Build: `docker build -t aurambaj/mojito-cli -f docker/Dockerfile-cli-bk8 .` and
push: `docker push aurambaj/mojito-cli:latest`

Example to run a command:
`docker run --rm --name mojito-cli -it -e MOJITO_HOST="mojito.org" -e MOJITO_PORT="443" -e MOJITO_SCHEME="https" aurambaj/mojito-cli repo-view -n demo1`

## Webapp image

Build: `docker build -t aurambaj/mojito-webapp -f docker/Dockerfile-bk8 .` and
push: `docker push aurambaj/mojito-webapp:latest`

Start the webapp: `docker run --rm --name mojito-webapp -it aurambaj/mojito-webapp` and
get a shell to try some command `docker exec -it mojito-webapp bash`

# Old notes

## May need extra env variable

Depending on the version of docker
`DOCKER_BUILDKIT=1 docker build -t aurambaj/mojito-webapp-bk8 -f docker/Dockerfile-bk8 .`

## Multi-stage build

To build manually `docker build -t aurambaj/mojito-webapp-ms8 -f docker/Dockerfile-ms8 .`

## Create image from already built jars

First build the Webapp with `mvn clean install -DskipTests`

In the `webapp/target` directory run `docker build -t aurambaj/mojito-webapp-old -f ../src/main/docker/Dockerfile .`

To test run it: `docker run -p 8080:8080 aurambaj/mojito-webapp-old:latest`

## Local builds using docker compose

Create a docker image only with the build binaries, and use docker-compose to build locally by mounting the source code
and `.m2` repository

`docker-compose -f docker/docker-compose-build.yml run compile`

### Depending on the docker installation on Mac

This is not needed with Docker Desktop for Mac but was needed before so for the reccord

### Find IP on Mac

Depending on how you instlled docker on Mac, `localhost` may not work. To get the IP to reach the
service: `docker-machine ip default`.

`export l10n_resttemplate_host="192.168.99.111" ` and then whatever CLI command you have to run, eg.
`mojito demo-create -n dockertest`

### Extra configuration for mysql

```
services:
    db:
    image: mysql:5.7
        user: "1000:50" # needed on Mac
        volumes:
          - "./.data/db:/var/lib/mysql"
        restart: always
        environment:
          MYSQL_ROOT_PASSWORD: ChangeMe
          MYSQL_DATABASE: mojito
          MYSQL_USER: mojito
          MYSQL_PASSWORD: ChangeMe
        command:
          - --character-set-server=utf8mb4
          - --collation-server=utf8mb4_bin
          - --innodb_use_native_aio=0 # needed on Mac
```

## Alpine version

`FROM adoptopenjdk:8-jre` --> `FROM adoptopenjdk/openjdk8:alpine-jre` and change to `echo -e` for script generation
and make sure to use `/bin/echo` for consistent behavior between alpine and ubuntu.

## Kubernetes

Be aware that Kubernetes injects environment variables in the container, and that some could conflict with the
ones defined in the `Dockerfile`. For example, `MOJITO_PORT` was used in the `Dockerfile` but it is also injected by
Kubernetes with a value like `tcp://{ip}:{port}` which was making the CLI command fail in the container. Replaced
`MOJITO_PORT` with `MOJITO_SERVER_PORT` in the `Dockerfile` and the CLI command.

### Port forwarding for MySQL

First forward the pod port to the localhost port, then in the container use `socat` to forward pod port to the remote MySQL
server. Finally connect the SQL client to the localhost port: `33306`.

```
kubectl port-forward svc/mojito-webapp 33306:3306
socat TCP-LISTEN:3306,reuseaddr,fork TCP:mojito-mysql-staging.mysql.database.azure.com:3306
```
