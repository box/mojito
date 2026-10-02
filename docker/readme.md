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

# Local MySQL schema upgrade smoke harness

`docker/docker-compose-mysql-smoke.yml` is a laptop stack for rehearsing MySQL schema upgrades before they are deployed. Start it by hand. It is separate from `docker-compose.yml` and `docker-compose-api-worker.yml`, and it is not part of the test run.

Run the commands below from the repository root.

The stack starts MySQL 8.0.34 (`utf8mb4` / `utf8mb4_bin`) and the Mojito webapp built from `docker/Dockerfile-bk21`. MySQL files are stored in the host directory `docker/.data/db`. That path is a bind-mount, so `docker compose down` removes the containers and leaves the directory on disk. Delete `docker/.data/db` only when a recipe says to start fresh.

The webapp is configured like a MySQL deploy: Flyway on, `l10n.flyway.clean=false`, Hibernate `ddl-auto=none`, the MySQL dialect, and the MySQL driver. `spring.jpa.defer-datasource-initialization` is false so the in-memory HSQL init scripts are not applied.

Username and password are the local Compose values `mojito` / `ChangeMe`. On Docker Desktop and Colima for Mac the database is started with `--innodb_use_native_aio=0`, which this bind-mount needs. Mail health is turned off in this stack (`management.health.mail.enabled=false`) because the default settings point mail at `localhost` and this stack has no mail server. Without that, `/actuator/health` stays `DOWN` even when the app and database are fine.

## Run it locally and add demo data

Stop any Mojito you started in IntelliJ or with `java -jar` before these commands. This stack publishes port 8080, and only one process can listen there. Run the commands from the repository root.

When you have changed Mojito code or migrations, run `docker/smoke/build-image.sh` before `up`. The first build compiles Mojito inside the image and takes a while. Later starts can use `up` alone when the recorded image is already the one you want to test.

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml up -d
```

Do not put `FLYWAY_TARGET=...` in front of this command. Unset means the latest schema. If `docker/.data/db` already contains a migrated database, Flyway sees `flyway_schema_history` and applies only scripts that are not in that logbook yet.

The image is a packaged snapshot of Mojito from the last Docker build. Editing a source or migration file does not update an existing image. `docker/smoke/build-image.sh` builds the webapp and writes `docker/.data/webapp-build.txt`. That record contains the image ID and a fingerprint of every `V*.sql` and `V*.java` migration file. `docker/smoke/check.sh` fails if the running container is a different image or those files changed after the build. It also compares the SQL files inside the running jar with the working tree. Java migrations are compiled into the jar, so that second comparison cannot see their `.java` files; the build record covers them. Deleting `docker/.data/db` does not delete the build record.

Follow the webapp log until it reports that the application started:

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml logs -f webapp
```

Confirm the process on port 8080 is healthy:

```bash
curl -sf http://127.0.0.1:8080/actuator/health
```

The body should contain `"status":"UP"`.

Open http://localhost:8080/login and sign in as `admin` / `ChangeMe`. That user is created when this database has no users yet.

Create the sample repository once the app is up:

```bash
docker/smoke/mojito-local demo-create -n Demo
```

`docker/smoke/mojito-local` is a script. It enters the running webapp container and runs the Mojito CLI there. The first line of output must be `mojito-local: webapp container at localhost:8080`. If the container is down, or its CLI target is anything else, the script exits and does not run a `mojito` installed on the host. `demo-create` is the CLI command that creates a repository with sample languages and translations. `-n Demo` sets the repository name. The same name a second time fails because that repository already exists. Pick another name, such as `Demo2`.

The repository rows are stored in MySQL under `docker/.data/db`, so they are still there after the containers stop. The sample `Demo/demo.properties` files are written inside the container, because that is where the CLI ran.

When you are finished:

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml down
```

`down` removes the containers and the network for this Compose file. It leaves `docker/.data/db` on disk. The next `up -d` still has the `Demo` repository. Delete `docker/.data/db` only when you want an empty database. After that delete, the next `up -d` migrates from the first script through the latest one. Still leave `FLYWAY_TARGET` unset.

`docker/smoke/check.sh` is optional here. It is a separate pass/fail script you run by hand after the app is up. It is not started by `up`.

## Smoke checks

`docker/smoke/check.sh` waits until the app is up, then checks:

1. `GET /actuator/health` returns status `UP`.
2. The running webapp is the image recorded by `docker/smoke/build-image.sh`, and the `V*.sql` and `V*.java` migration files still match that build. The SQL files inside the running jar are also compared with the working tree.
3. `MAX(version)` in `flyway_schema_history` equals the highest versioned migration Flyway loads: `V*.sql` in `webapp/src/main/resources/db/migration/` and `V*.java` in `webapp/src/main/java/db/migration/`. This comparison assumes every version is a plain integer. See "Migration versions are integers" below.
4. Repo-type create, list, update, view, and delete succeed through `docker/smoke/mojito-local`: `repo-type-create`, `repo-type-list`, `repo-type-update`, `repo-type-view`, and `repo-type-delete`. After update, the view output must contain the new description. After delete, a second list must not contain that name. The script creates a new name on every run (`local-smoke-<timestamp>-<pid>`), so the delete step can only remove the type that run just created. The Compose file sets `MOJITO_HOST=localhost`, `MOJITO_SCHEME=http`, and `MOJITO_PORT=8080` for the CLI inside the image. `mojito-local` refuses to run if the container reports any other target.

### Migration versions are integers

Mojito names each versioned migration with a plain integer and two underscores, such as `V69__Add_repo_type.sql` and `V9__Compute_Word_Count.java`. Keep new migrations on the next integer (`V70__...`, then `V71__...`). If a change needs a dotted version or a repeatable script, update `docker/smoke/check.sh` before treating a green smoke result as proof that the new script was applied.

## Recipe: fresh database

Deletes `docker/.data/db`, then migrates V1 through the latest script.

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml down
rm -rf docker/.data/db
mkdir -p docker/.data/db
docker/smoke/build-image.sh
docker compose -f docker/docker-compose-mysql-smoke.yml up -d
docker/smoke/check.sh
```

`build-image.sh` compiles the current Mojito files into the image and records that build. This takes a while. Do not replace it with `up --build`: that can start a new image without updating `docker/.data/webapp-build.txt`, and the smoke check then fails.

## Stopping at a Flyway version

`FLYWAY_TARGET` is passed through to Mojito as `spring.flyway.target`. When it is unset, the value is `latest` and Flyway applies every pending script. Set it to a version number to migrate up to that version and stop. This uses Mojito's own Flyway inside the webapp process. Java migrations `V9__Compute_Word_Count` and `V56__TUCVAddAssetIdUpdater` run only there.

A first boot at an older target may exit after Flyway, because Hibernate expects the latest schema. That is acceptable. The database version is the goal of that boot. Confirm it in `flyway_schema_history`, then boot again with `FLYWAY_TARGET` unset so Flyway applies only the remaining scripts. After that second boot the app must stay up.

Pass `FLYWAY_TARGET` only on the command that should stop early. The follow-up `docker compose up` must not include it, or the same cap is applied again.

For a normal local session, leave `FLYWAY_TARGET` unset. The database migrates to the latest script and the app stays up. Set a version only when you are rehearsing a stop-partway upgrade (the recipe below).

## Recipe: upgrade from version N to latest

Start fresh, migrate through version N, and boot again with no target only when the logbook stopped at exactly N. The example uses N=66. Use any version below the latest script.

Paste this as one block. The second boot and `docker/smoke/check.sh` are inside the success branch. `check.sh` only sees the final version, so after a second boot it cannot tell a fresh migrate from an upgrade. If the logbook is already past N, or it never reaches N, the block prints an error and stops. The webapp is not recreated, and the smoke check does not run. A version already past N will not become N, so the wait ends as soon as that shows up instead of running out the clock.

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml down
rm -rf docker/.data/db
mkdir -p docker/.data/db
TARGET=66
# Build once before either boot so both boots run the current source and migrations.
docker/smoke/build-image.sh
FLYWAY_TARGET="$TARGET" docker compose -f docker/docker-compose-mysql-smoke.yml up -d

# Wait until the logbook shows exactly TARGET. The webapp may exit after Flyway; the db container stays up.
version=""
for _ in $(seq 1 60); do
  version="$(docker compose -f docker/docker-compose-mysql-smoke.yml exec -T db \
    mysql -N -umojito -pChangeMe mojito \
    -e "SELECT IFNULL(MAX(CAST(version AS UNSIGNED)), 0) FROM flyway_schema_history WHERE success = 1;" \
    2>/dev/null || true)"
  version="${version//[[:space:]]/}"
  echo "flyway version: ${version:-unknown}"
  if [ "$version" = "$TARGET" ]; then
    break
  fi
  case "$version" in
    ''|*[!0-9]*)
      ;;
    *)
      if [ "$version" -gt "$TARGET" ]; then
        break
      fi
      ;;
  esac
  sleep 10
done

if [ "$version" != "$TARGET" ]; then
  echo "Did not stop at ${TARGET} (saw '${version:-unknown}'). Refusing the second boot." >&2
  false
else
  # Second boot: no FLYWAY_TARGET, so the cap is latest. Leave docker/.data/db in place.
  docker compose -f docker/docker-compose-mysql-smoke.yml up -d --no-deps --force-recreate webapp
  docker/smoke/check.sh
  docker compose -f docker/docker-compose-mysql-smoke.yml down
fi
```

`false` makes the block finish with a failing status. It does not close an interactive terminal. `docker compose down` runs only after the smoke check passes. It removes the containers and keeps `docker/.data/db`. If the recipe stops early, the containers stay up so you can read the logbook.
