#!/usr/bin/env bash
# Smoke checks for the local MySQL schema-upgrade stack.
# Run from anywhere after the webapp has been started at the latest Flyway version.
#
#   docker/smoke/check.sh
#
# Checks:
#   1. GET /actuator/health reports UP
#   2. The running webapp is the image recorded by docker/smoke/build-image.sh,
#      and the SQL and Java migration sources still match that build
#   3. SQL migrations packaged in the running webapp match the working tree
#   4. flyway_schema_history max version equals the highest versioned migration
#      Flyway loads: V*.sql under webapp/src/main/resources/db/migration/ and
#      V*.java under webapp/src/main/java/db/migration/
#   5. repo-type-create, repo-type-list, repo-type-update, repo-type-view,
#      and repo-type-delete succeed. A second list must not contain the
#      deleted name.
#
# CLI calls go through docker/smoke/mojito-local, which runs mojito inside the webapp
# container and refuses any target other than localhost:8080. The script never calls a
# mojito installed on the host.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/docker/smoke/lib.sh"
COMPOSE_FILE="$ROOT/docker/docker-compose-mysql-smoke.yml"
BUILD_RECORD="$ROOT/docker/.data/webapp-build.txt"
SQL_MIGRATION_DIR="$ROOT/webapp/src/main/resources/db/migration"
JAVA_MIGRATION_DIR="$ROOT/webapp/src/main/java/db/migration"
BASE_URL="http://127.0.0.1:8080"
# Unique per run, so delete can only remove the type this run created.
REPO_TYPE_NAME="local-smoke-$(date +%Y%m%d%H%M%S)-$$"
UPDATED_DESCRIPTION="local smoke check updated"

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

container_cli() {
  "$ROOT/docker/smoke/mojito-local" "$@"
}

require_sha256

if [[ ! -d "$SQL_MIGRATION_DIR" ]]; then
  echo "Migration directory not found: $SQL_MIGRATION_DIR" >&2
  exit 1
fi

if [[ ! -d "$JAVA_MIGRATION_DIR" ]]; then
  echo "Migration directory not found: $JAVA_MIGRATION_DIR" >&2
  exit 1
fi

# Flyway's default location is classpath:db/migration. That loads SQL from
# resources and Java classes from the same package. A Java-only migration
# numbered above every SQL file is still the latest version in the database.
expected="$(
  {
    find "$SQL_MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.sql' -print
    find "$JAVA_MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.java' -print
  } \
    | sed -nE 's|.*/V([0-9]+)__.*|\1|p' \
    | sort -n \
    | tail -n 1
)"

if [[ -z "$expected" ]]; then
  echo "No V* migrations found in $SQL_MIGRATION_DIR or $JAVA_MIGRATION_DIR" >&2
  exit 1
fi

echo "Waiting for ${BASE_URL}/actuator/health"
health=""
for _ in $(seq 1 90); do
  if health="$(curl -sf --max-time 5 "${BASE_URL}/actuator/health")"; then
    break
  fi
  health=""
  sleep 5
done

if [[ -z "$health" ]]; then
  echo "Webapp did not become healthy at ${BASE_URL}/actuator/health" >&2
  exit 1
fi

echo "$health"
printf '%s\n' "$health" | grep -q '"status":"UP"'

# Java migrations are compiled before they enter the image, so their .java
# files are not inside the jar. build-image.sh records the source fingerprint
# and the image ID. This fails when either the sources or the running image
# differ from that record.
if [[ ! -f "$BUILD_RECORD" ]]; then
  echo "No webapp build record at docker/.data/webapp-build.txt." >&2
  echo "Run docker/smoke/build-image.sh before this check." >&2
  exit 1
fi

recorded_image_id="$(awk -F= '/^image_id=/ { print $2; exit }' "$BUILD_RECORD")"
recorded_fingerprint="$(awk -F= '/^fingerprint=/ { print $2; exit }' "$BUILD_RECORD")"
recorded_manifest="$(tail -n +3 "$BUILD_RECORD")"
current_manifest="$(migration_manifest "$ROOT")"
current_fingerprint="$(migration_fingerprint "$current_manifest")"

if [[ -z "$recorded_image_id" || -z "$recorded_fingerprint" ]]; then
  echo "The webapp build record is incomplete. Run docker/smoke/build-image.sh again." >&2
  exit 1
fi

if [[ "$current_fingerprint" != "$recorded_fingerprint" || "$current_manifest" != "$recorded_manifest" ]]; then
  echo "SQL or Java migration files changed after the webapp image was built." >&2
  echo "Run docker/smoke/build-image.sh, then start the webapp again." >&2
  diff -u \
    <(printf '%s\n' "$recorded_manifest") \
    <(printf '%s\n' "$current_manifest") \
    >&2 || true
  exit 1
fi

webapp_container_id="$(compose ps -q webapp)"
webapp_container_id="${webapp_container_id%%$'\n'*}"
if [[ -z "$webapp_container_id" ]]; then
  echo "Webapp container is not running." >&2
  exit 1
fi
running_image_id="$(docker inspect --format '{{.Image}}' "$webapp_container_id")"
if [[ "$running_image_id" != "$recorded_image_id" ]]; then
  echo "The running webapp container is not the image recorded by docker/smoke/build-image.sh." >&2
  echo "Recorded: $recorded_image_id" >&2
  echo "Running:  $running_image_id" >&2
  echo "Run docker/smoke/build-image.sh, then start the webapp again." >&2
  exit 1
fi
echo "Running webapp image matches the SQL and Java migrations from its build."

# A Docker image is a snapshot from its last build. Compare the SQL files in
# the running jar with the working tree so an old image cannot produce a green
# smoke result after a migration was edited without rebuilding.
source_sql_manifest="$(
  while IFS= read -r -d '' migration; do
    printf '%s  %s\n' "$(sha256_file "$migration")" "$(basename "$migration")"
  done < <(find "$SQL_MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.sql' -print0) \
    | sort
)"

if ! image_sql_manifest="$(
  # Quoted heredoc: the laptop shell must not expand these variables.
  # The shell inside the webapp container expands them.
  compose exec -T webapp sh -s <<'END_IMAGE_MANIFEST'
set -eu
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cd "$tmp"
jar xf "$MOJITO_BIN/mojito-webapp.jar" BOOT-INF/classes/db/migration
find BOOT-INF/classes/db/migration -maxdepth 1 -type f -name 'V*.sql' \
  -exec sha256sum {} \; \
  | while read -r checksum path; do
      printf '%s  %s\n' "$checksum" "$(basename "$path")"
    done \
  | sort
END_IMAGE_MANIFEST
)"; then
  echo "Could not read SQL migrations from the running webapp image." >&2
  exit 1
fi

if [[ "$source_sql_manifest" != "$image_sql_manifest" ]]; then
  echo "The running webapp image does not contain the SQL migrations in this working tree." >&2
  echo "Run docker/smoke/build-image.sh, then start the webapp again." >&2
  diff -u \
    <(printf '%s\n' "$image_sql_manifest") \
    <(printf '%s\n' "$source_sql_manifest") \
    >&2 || true
  exit 1
fi
echo "Running webapp SQL migrations match the working tree."

# Keep the MySQL password warning off the success path, but keep the real
# error when the container, login, or history table cannot be read.
query_err="$(mktemp)"
if ! actual="$(
  compose exec -T db \
    mysql -N -umojito -pChangeMe mojito \
    -e "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1;" \
    2>"$query_err"
)"; then
  echo "Could not read flyway_schema_history from the smoke database." >&2
  if [[ -s "$query_err" ]]; then
    cat "$query_err" >&2
  fi
  rm -f "$query_err"
  exit 1
fi
rm -f "$query_err"
actual="${actual//[[:space:]]/}"

echo "flyway_schema_history max version: ${actual:-<empty>} (highest migration: ${expected})"
if [[ "$actual" != "$expected" ]]; then
  echo "Flyway version does not match the highest migration." >&2
  exit 1
fi

echo "repo-type-create ${REPO_TYPE_NAME}"
container_cli repo-type-create -n "$REPO_TYPE_NAME" -d "local smoke check"

echo "repo-type-list"
list_output="$(container_cli repo-type-list)"
printf '%s\n' "$list_output"
printf '%s\n' "$list_output" | grep -q "$REPO_TYPE_NAME"

echo "repo-type-update ${REPO_TYPE_NAME}"
container_cli repo-type-update -n "$REPO_TYPE_NAME" -d "$UPDATED_DESCRIPTION"

echo "repo-type-view ${REPO_TYPE_NAME}"
view_output="$(container_cli repo-type-view -n "$REPO_TYPE_NAME")"
printf '%s\n' "$view_output"
printf '%s\n' "$view_output" | grep -q "$REPO_TYPE_NAME"
printf '%s\n' "$view_output" | grep -q "$UPDATED_DESCRIPTION"

echo "repo-type-delete ${REPO_TYPE_NAME}"
container_cli repo-type-delete -n "$REPO_TYPE_NAME"

echo "repo-type-list after delete"
list_after="$(container_cli repo-type-list)"
printf '%s\n' "$list_after"
if printf '%s\n' "$list_after" | grep -q "$REPO_TYPE_NAME"; then
  echo "repo-type-delete left ${REPO_TYPE_NAME} in the list." >&2
  exit 1
fi

echo "Smoke checks passed (Flyway version ${actual})."
