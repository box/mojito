#!/usr/bin/env bash
# Smoke checks for the local MySQL schema-upgrade stack.
# Run from anywhere after the webapp has been started at the latest Flyway version.
#
#   docker/smoke/check.sh
#
# Checks:
#   1. GET /actuator/health reports UP
#   2. flyway_schema_history max version equals the highest V* script
#      under webapp/src/main/resources/db/migration/
#   3. repo-type-create, repo-type-list, repo-type-view, repo-type-delete succeed
#
# CLI calls go through docker/smoke/mojito-local, which runs mojito inside the webapp
# container and refuses any target other than localhost:8080. The script never calls a
# mojito installed on the host.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILE="$ROOT/docker/docker-compose-mysql-smoke.yml"
MIGRATION_DIR="$ROOT/webapp/src/main/resources/db/migration"
BASE_URL="http://127.0.0.1:8080"
# Unique per run, so delete can only remove the type this run created.
REPO_TYPE_NAME="local-smoke-$(date +%Y%m%d%H%M%S)-$$"

compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

container_cli() {
  "$ROOT/docker/smoke/mojito-local" "$@"
}

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "Migration directory not found: $MIGRATION_DIR" >&2
  exit 1
fi

expected="$(
  find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.sql' -print \
    | sed -E 's|.*/V([0-9]+)__.*|\1|' \
    | sort -n \
    | tail -n 1
)"

if [[ -z "$expected" ]]; then
  echo "No V* migrations found in $MIGRATION_DIR" >&2
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

actual="$(
  compose exec -T db \
    mysql -N -umojito -pChangeMe mojito \
    -e "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1;" \
    2>/dev/null || true
)"
actual="${actual//[[:space:]]/}"

echo "flyway_schema_history max version: ${actual:-<empty>} (highest migration script: ${expected})"
if [[ "$actual" != "$expected" ]]; then
  echo "Flyway version does not match the highest migration script." >&2
  exit 1
fi

echo "repo-type-create ${REPO_TYPE_NAME}"
container_cli repo-type-create -n "$REPO_TYPE_NAME" -d "local smoke check"

echo "repo-type-list"
list_output="$(container_cli repo-type-list)"
printf '%s\n' "$list_output"
printf '%s\n' "$list_output" | grep -q "$REPO_TYPE_NAME"

echo "repo-type-view ${REPO_TYPE_NAME}"
view_output="$(container_cli repo-type-view -n "$REPO_TYPE_NAME")"
printf '%s\n' "$view_output"
printf '%s\n' "$view_output" | grep -q "$REPO_TYPE_NAME"

echo "repo-type-delete ${REPO_TYPE_NAME}"
container_cli repo-type-delete -n "$REPO_TYPE_NAME"

echo "Smoke checks passed (Flyway version ${actual})."
