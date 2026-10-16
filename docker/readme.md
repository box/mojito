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

`export l10n_resttemplate_host="192.168.99.111"`  and then whatever CLI command you have to run, eg.
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

`docker/docker-compose-mysql-smoke.yml` is for rehearsing MySQL schema upgrades locally before they are deployed. Start it by hand. It is separate from `docker-compose.yml` and `docker-compose-api-worker.yml`, and it is not part of the test run.

Run the commands below from the repository root.

The stack starts MySQL 8.0.34 (`utf8mb4` / `utf8mb4_bin`) and the Mojito webapp built from `docker/Dockerfile-bk21`. MySQL files are stored in the host directory `docker/.data/db`. That path is a bind-mount, so `docker compose down` removes the containers and leaves the directory on disk. Delete `docker/.data/db` only when a recipe says to start fresh.

The webapp uses the same database settings as a MySQL-backed Mojito instance: Flyway on, `l10n.flyway.clean=false`, Hibernate `ddl-auto=none`, the MySQL dialect, and the MySQL driver. `spring.jpa.defer-datasource-initialization` is false so the in-memory HSQL init scripts are not applied.

Username and password are the local Compose values `mojito` / `ChangeMe`. On Docker Desktop and Colima for Mac the database is started with `--innodb_use_native_aio=0`, which this bind-mount needs. Mail health is turned off in this stack (`management.health.mail.enabled=false`) because the default settings point mail at `localhost` and this stack has no mail server.

## Run it locally and add demo data

Stop any local Mojito instance (IntelliJ or `java -jar`) first. This stack binds host port 8080.

When Mojito code or migrations change, run `docker/smoke/build-image.sh` before `up`. That script compiles Mojito into the image. If the recorded image is already the one under test, later starts need only `up`.

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml up -d
```

This command leaves `FLYWAY_TARGET` unset, so Flyway migrates to the latest script in the running image. To rehearse an upgrade of a populated database, use [Recipe: upgrade from version N to latest](#recipe-upgrade-from-version-n-to-latest). If `docker/.data/db` already contains a migrated database, Flyway reads `flyway_schema_history` and applies only scripts that are not in that logbook yet.

The image is a packaged snapshot of Mojito from the last Docker build. Editing a source or migration file does not update an existing image. `docker/smoke/build-image.sh` builds the webapp and writes `docker/.data/webapp-build.txt`. That record contains the image ID and a fingerprint of every `V*.sql` and `V*.java` migration file. `docker/smoke/check.sh` fails if the running container is a different image or those files changed after the build. It also compares SQL in the running jar with the working tree. Java migrations are compiled to class files and are not shipped as `.java`; `check.sh` verifies them through that fingerprint instead. Deleting `docker/.data/db` does not delete the build record. To try a host-built jar without that full rebuild, see [Edit loop without rebuilding the image](#edit-loop-without-rebuilding-the-image). That path is not what `check.sh` trusts.

Follow the webapp log until it reports that the application started:

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml logs -f webapp
```

Confirm the process on port 8080 is healthy:

```bash
curl -sf http://127.0.0.1:8080/actuator/health
```

The body should contain `"status":"UP"`.

Open [http://localhost:8080/login](http://localhost:8080/login) and sign in with the bootstrap user from `l10n.bootstrap.user` (created when this database has no users yet).

Create the sample repository once the app is up:

```bash
docker/smoke/mojito-local demo-create -n Demo
```

`docker/smoke/mojito-local` runs the Mojito CLI inside the webapp container. The first line of output must be `mojito-local: webapp container at localhost:8080`. `demo-create -n Demo` creates a repository named Demo with sample languages and translations.

When you are finished:

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml down
```

`down` removes the containers and the Compose network. MySQL files remain in `docker/.data/db`, so the next `up -d` reuses that database. Delete `docker/.data/db` only to start from an empty schema; the following `up -d` then applies every migration.

`docker/smoke/check.sh` is optional here. It is a separate pass/fail script you run by hand after the app is up. It is not started by `up`.

## Edit loop without rebuilding the image

`build-image.sh` is the run `check.sh` can trust. This shortcut is for trying a Java or migration change without that full image build. MySQL and `docker/.data/db` stay up.

The running image has to honor `MOJITO_WEBAPP_JAR` and `MOJITO_CLI_JAR`. Run `docker/smoke/build-image.sh` once after that change to the image. Later tries can skip the image build.

Compile on the laptop, copy the jars onto stable names, and recreate only the webapp with the extra Compose file:

```bash
mvn -DskipTests package
docker/smoke/stage-local-jars.sh
docker compose \
  -f docker/docker-compose-mysql-smoke.yml \
  -f docker/docker-compose-mysql-smoke.local-jars.yml \
  up -d --no-deps --force-recreate webapp
```

`stage-local-jars.sh` copies `mojito-webapp-*-exec.jar` and `mojito-cli-*-exec.jar` to `docker/local-jars/mojito-webapp.jar` and `docker/local-jars/mojito-cli.jar`. Compose cannot mount a glob, and mounting over `/usr/local/mojito/bin` would hide the `mojito` wrapper that `mojito-local` calls. The Compose file mounts `docker/local-jars` at `/opt/mojito-jars` instead. Copy again after every Maven build, then recreate the webapp. The process keeps the jar it opened at start. A host-built jar is fine here. Do not use this shortcut to supply `webapp/node`.

`check.sh` fails while this mount is in place. The image id does not change, so a green check would not describe the jars that are actually running. When you want that check, recreate the webapp from the baked image:

```bash
docker compose -f docker/docker-compose-mysql-smoke.yml up -d --no-deps --force-recreate webapp
```

That command does not include the local-jars file, so the container goes back to the jars inside the image. Run `docker/smoke/build-image.sh` first if the sources have moved on from that image.

## Smoke checks

`docker/smoke/check.sh` waits until the app is up, then verifies:

1. `GET /actuator/health` reports `UP`.
2. The running webapp matches the image and migration fingerprint recorded by `docker/smoke/build-image.sh`. SQL in the running jar is also compared with the working tree. The check fails if host jars are mounted with `docker-compose-mysql-smoke.local-jars.yml`.
3. `MAX(version)` in `flyway_schema_history` equals the highest integer migration Flyway loads (`V*.sql` under `webapp/src/main/resources/db/migration/` and `V*.java` under `webapp/src/main/java/db/migration/`). See [Migration versions are integers](#migration-versions-are-integers).
4. Repo-type create, list, update, view, and delete succeed via `docker/smoke/mojito-local`. Each run uses a unique name. Update is confirmed in `repo-type-view`; delete is confirmed by a second list that omits that name.

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

## Upgrade from an older Mojito

`FLYWAY_TARGET` maps to `spring.flyway.target`. Leave it unset. Flyway then applies every pending script in the image that is running, through that image's own latest migration.

An upgrade rehearsal is two images on one `docker/.data/db`. The first image is the commit that introduced migration N, so N is that build's latest migration. Code and schema match, and the app stays up. The second image is the Mojito on this branch. It applies N+1 through the latest script to a database that already holds a repository, an asset, and an imported translation.

The example uses N=66. Pick a version below the latest script on this branch, from a commit `docker/Dockerfile-bk21` can compile (Java 21). That commit does not need the smoke harness. This branch's Dockerfile builds it and installs the `mojito` command. `docker/smoke/mojito-local` runs that command from the host.

The seed is `repo-create`, `push` of `docker/smoke/fixtures/populated-upgrade/messages.properties`, and `import` of `messages_fr-FR.properties`. The CLI runs inside the container, so the recipe copies those files in with `docker compose cp`. Record the repository id from `repo-view` and the translated string `Bonjour de la mise à niveau du schéma` from `pull` before the second boot, and require the same id and string after it. `docker/smoke/check.sh` runs only after that. It sees the final version, so it cannot tell this populated upgrade from a fresh migrate.

Do not call `repo-type-create` on the first boot when N is below V69. That table is not there yet. `check.sh` covers the repository-type API after the upgrade.

## Recipe: upgrade from version N to latest

Run this with bash from the repository root. Both image builds compile Mojito and take a while. The first `docker build` does not update `docker/.data/webapp-build.txt`. `build-image.sh` records the second image, which is the one `check.sh` trusts.

The block checks the seed before it recreates the webapp. If that check fails, it does not build the current image or run `check.sh`. On success it removes the containers. `docker/.data/db` stays on disk. The temporary git worktree is removed when the block ends, including when it stops early. If it stops early, the stack stays up so you can read the webapp log.

```bash
set -euo pipefail
unset FLYWAY_TARGET

COMPOSE=(docker compose -f docker/docker-compose-mysql-smoke.yml)
TARGET=66
REPO=schema-upgrade-smoke
TRANSLATED='Bonjour de la mise à niveau du schéma'
FIXTURE=docker/smoke/fixtures/populated-upgrade
IMAGE=mojito-mysql-smoke-webapp:latest

latest_migration_at() {
  git ls-tree -r --name-only "$1" -- \
    webapp/src/main/resources/db/migration \
    webapp/src/main/java/db/migration \
    | sed -nE 's|.*/V([0-9]+)__.*|\1|p' \
    | sort -n \
    | tail -n 1
}

strip_ansi() {
  perl -pe 's/\e\[[0-9;]*[A-Za-z]//g'
}

wait_until_up() {
  local health=""
  echo "Waiting for http://127.0.0.1:8080/actuator/health"
  for _ in $(seq 1 90); do
    if health="$(curl -sf --max-time 5 http://127.0.0.1:8080/actuator/health)"; then
      if printf '%s\n' "$health" | grep -q '"status":"UP"'; then
        echo "$health"
        return 0
      fi
    fi
    sleep 5
  done
  echo "Webapp did not stay up at http://127.0.0.1:8080/actuator/health" >&2
  return 1
}

flyway_version() {
  local query_err raw
  if ! query_err="$(mktemp)"; then
    echo "Could not read flyway_schema_history." >&2
    return 1
  fi
  if ! raw="$(
    "${COMPOSE[@]}" exec -T db \
      mysql -N -umojito -pChangeMe mojito \
      -e "SELECT IFNULL(MAX(CAST(version AS UNSIGNED)), 0) FROM flyway_schema_history WHERE success = 1;" \
      2>"$query_err"
  )"; then
    echo "Could not read flyway_schema_history." >&2
    if [[ -s "$query_err" ]]; then
      cat "$query_err" >&2
    fi
    rm -f "$query_err"
    return 1
  fi
  rm -f "$query_err"
  printf '%s\n' "${raw//[[:space:]]/}"
}

copy_fixtures() {
  "${COMPOSE[@]}" exec -T webapp mkdir -p /opt/smoke-seed /opt/smoke-pull
  "${COMPOSE[@]}" exec -T webapp rm -rf /opt/smoke-pull
  "${COMPOSE[@]}" exec -T webapp mkdir -p /opt/smoke-pull
  "${COMPOSE[@]}" cp "$FIXTURE/messages.properties" webapp:/opt/smoke-seed/messages.properties
  "${COMPOSE[@]}" cp "$FIXTURE/messages_fr-FR.properties" webapp:/opt/smoke-seed/messages_fr-FR.properties
}

# Prints the repository id on stdout. repo-view and pull are shown on stderr.
# Fails unless pull wrote TRANSLATED.
assert_seed() {
  local view repo_id
  if ! view="$(docker/smoke/mojito-local repo-view -n "$REPO" | strip_ansi)"; then
    echo "repo-view failed for ${REPO}." >&2
    return 1
  fi
  printf '%s\n' "$view" >&2
  repo_id="$(printf '%s\n' "$view" | sed -nE 's/.*Repository id --> ([0-9]+).*/\1/p')"
  repo_id="${repo_id%%$'\n'*}"
  if [[ -z "$repo_id" ]]; then
    echo "repo-view did not print a repository id for ${REPO}." >&2
    return 1
  fi
  if ! docker/smoke/mojito-local pull -r "$REPO" -s /opt/smoke-seed -t /opt/smoke-pull >&2; then
    echo "pull failed for ${REPO}." >&2
    return 1
  fi
  if ! "${COMPOSE[@]}" exec -T webapp grep -R -F "$TRANSLATED" /opt/smoke-pull >&2; then
    echo "Imported translation was not in the pull output." >&2
    return 1
  fi
  printf '%s\n' "$repo_id"
}

"${COMPOSE[@]}" down
rm -rf docker/.data/db
mkdir -p docker/.data/db

current_latest="$(
  find webapp/src/main/resources/db/migration webapp/src/main/java/db/migration \
    -maxdepth 1 -type f \( -name 'V*.sql' -o -name 'V*.java' \) -print \
    | sed -nE 's|.*/V([0-9]+)__.*|\1|p' \
    | sort -n \
    | tail -n 1
)"
if [[ -z "$current_latest" || "$TARGET" -ge "$current_latest" ]]; then
  echo "TARGET=${TARGET} must be below this branch's latest migration (${current_latest:-none})." >&2
  exit 1
fi

migration_files="$(
  find webapp/src/main/resources/db/migration webapp/src/main/java/db/migration \
    -maxdepth 1 -type f \( -name "V${TARGET}__*.sql" -o -name "V${TARGET}__*.java" \) | sort
)"
if [[ -z "$migration_files" ]]; then
  echo "No V${TARGET}__ migration file in this branch." >&2
  exit 1
fi

COMMIT=""
while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  candidate="$(git log --diff-filter=A -1 --format=%H -- "$file")"
  [[ -n "$candidate" ]] || continue
  if [[ "$(latest_migration_at "$candidate")" == "$TARGET" ]]; then
    COMMIT="$candidate"
    break
  fi
done <<EOF
$migration_files
EOF

if [[ -z "$COMMIT" ]]; then
  echo "No commit that added V${TARGET} has latest migration ${TARGET}." >&2
  exit 1
fi

WORKTREE="$(mktemp -d "${TMPDIR:-/tmp}/mojito-schema-${TARGET}.XXXXXX")"
rmdir "$WORKTREE"
cleanup_worktree() {
  git worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true
  git worktree prune >/dev/null 2>&1 || true
}
trap cleanup_worktree EXIT
git worktree add --detach "$WORKTREE" "$COMMIT"

java_version="$(sed -nE 's|.*<java.version>([^<]+)</java.version>.*|\1|p' "$WORKTREE/pom.xml")"
java_version="${java_version%%$'\n'*}"
if [[ "$java_version" != "21" ]]; then
  echo "Commit ${COMMIT} uses Java ${java_version:-unknown}. docker/Dockerfile-bk21 compiles Java 21." >&2
  exit 1
fi

# A worktree .git is a file (gitdir: <host path>). Exclude it so COPY . does not
# send that dangling pointer into the image. Create or append only in this
# temporary worktree; do not replace rules already in .dockerignore.
if [[ ! -e "$WORKTREE/.dockerignore" ]]; then
  printf '%s\n' '.git' > "$WORKTREE/.dockerignore"
elif ! grep -qxF '.git' "$WORKTREE/.dockerignore"; then
  printf '\n%s\n' '.git' >> "$WORKTREE/.dockerignore"
fi

echo "Building ${COMMIT} (latest migration V${TARGET}) as ${IMAGE}"
DOCKER_BUILDKIT=1 docker build -f docker/Dockerfile-bk21 -t "$IMAGE" "$WORKTREE"

# --no-build keeps Compose from replacing this image with a build of the current branch.
"${COMPOSE[@]}" up -d --no-build
wait_until_up
if ! version="$(flyway_version)"; then
  exit 1
fi
echo "flyway version: ${version:-unknown}"
if [[ "$version" != "$TARGET" ]]; then
  echo "First boot stopped at '${version:-unknown}', expected ${TARGET}. Refusing to seed." >&2
  exit 1
fi

copy_fixtures
docker/smoke/mojito-local repo-create -n "$REPO" -l fr-FR
docker/smoke/mojito-local push -r "$REPO" -s /opt/smoke-seed
docker/smoke/mojito-local import -r "$REPO" -s /opt/smoke-seed -t /opt/smoke-seed
if ! repo_id_before="$(assert_seed)"; then
  exit 1
fi
echo "Recorded repository id ${repo_id_before} and translation: ${TRANSLATED}"

# Second boot: current branch, same docker/.data/db, FLYWAY_TARGET still unset.
docker/smoke/build-image.sh
"${COMPOSE[@]}" up -d --no-deps --force-recreate --no-build webapp
wait_until_up
copy_fixtures
if ! repo_id_after="$(assert_seed)"; then
  exit 1
fi
if [[ "$repo_id_after" != "$repo_id_before" ]]; then
  echo "Repository id changed from ${repo_id_before} to ${repo_id_after}." >&2
  exit 1
fi
echo "Repository ${repo_id_after} and translation survived the upgrade."

docker/smoke/check.sh
"${COMPOSE[@]}" down
```
