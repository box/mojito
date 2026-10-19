#!/usr/bin/env bash
# Rehearse a MySQL schema upgrade against a populated repository.
#
#   docker/smoke/populated-upgrade.sh [N]
#
# N is the original schema, not the version the upgrade finishes on. It
# defaults to 66. The first image is the commit that introduced V{N}. The second
# image is this branch and applies every later migration to the same database.
# See docker/readme.md, "Recipe: upgrade from version N to latest".

set -euo pipefail
unset FLYWAY_TARGET

if [[ $# -gt 1 ]]; then
  echo "Usage: docker/smoke/populated-upgrade.sh [N]" >&2
  echo "N must be an integer migration version and defaults to 66." >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

COMPOSE=(docker compose -f docker/docker-compose-mysql-smoke.yml)
TARGET="${1:-66}"
if [[ ! "$TARGET" =~ ^[0-9]+$ ]]; then
  echo "Usage: docker/smoke/populated-upgrade.sh [N]" >&2
  echo "N must be an integer migration version and defaults to 66." >&2
  exit 1
fi
REPO=schema-upgrade-smoke
TRANSLATED='Bonjour de la mise à niveau du schéma'
FIXTURE=docker/smoke/fixtures/populated-upgrade
IMAGE=mojito-mysql-smoke-webapp:latest
DB_DIR=docker/.data/db

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
      </dev/null \
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
  # Source and translation stay in different directories. pull scans -s and
  # writes into a different -t, so a translation left beside the source is
  # scanned as its own source asset.
  "${COMPOSE[@]}" exec -T webapp mkdir -p /opt/smoke-source /opt/smoke-import /opt/smoke-pull </dev/null
  "${COMPOSE[@]}" exec -T webapp rm -rf /opt/smoke-pull </dev/null
  "${COMPOSE[@]}" exec -T webapp mkdir -p /opt/smoke-pull </dev/null
  "${COMPOSE[@]}" cp "$FIXTURE/messages.properties" webapp:/opt/smoke-source/messages.properties
  "${COMPOSE[@]}" cp "$FIXTURE/messages_fr-FR.properties" webapp:/opt/smoke-import/messages_fr-FR.properties
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
  if ! docker/smoke/mojito-local pull -r "$REPO" -s /opt/smoke-source -t /opt/smoke-pull >&2; then
    echo "pull failed for ${REPO}." >&2
    return 1
  fi
  if ! "${COMPOSE[@]}" exec -T webapp grep -R -F "$TRANSLATED" /opt/smoke-pull </dev/null >&2; then
    echo "Imported translation was not in the pull output." >&2
    return 1
  fi
  printf '%s\n' "$repo_id"
}

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

echo "Checks passed for V${TARGET} (${COMMIT})."
echo "${DB_DIR} holds the MySQL data and survives docker compose down."
echo "The first boot has to migrate an empty database to V${TARGET}. Flyway records applied versions in this directory, so a leftover directory makes this run continue from the schema already stored there."
# Read the terminal, not standard input.
printf 'Type yes or y to delete %s: ' "$DB_DIR"
if ! IFS= read -r confirmation </dev/tty; then
  echo "Refusing to delete ${DB_DIR}: no confirmation was read." >&2
  exit 1
fi
if [[ "$confirmation" != "yes" && "$confirmation" != "y" ]]; then
  echo "Aborted. ${DB_DIR} was not deleted." >&2
  exit 1
fi
"${COMPOSE[@]}" down
rm -rf "$DB_DIR"
mkdir -p "$DB_DIR"

# A worktree .git is a file (gitdir: <host path>). Exclude it so COPY . does not
# send that dangling pointer into the image. Create or append only in this
# temporary worktree; do not replace rules already in .dockerignore.
if [[ ! -e "$WORKTREE/.dockerignore" ]]; then
  printf '%s\n' '.git' > "$WORKTREE/.dockerignore"
elif ! grep -qxF '.git' "$WORKTREE/.dockerignore"; then
  printf '\n%s\n' '.git' >> "$WORKTREE/.dockerignore"
fi

# Only the first image needs a stand-in git.properties. That image is the
# commit that introduced migration N (COMMIT above; V66 in the example).
# Its webapp and CLI load classpath:git.properties at startup and exit if
# the file is missing. git-commit-id would write it, but this worktree's .git
# file points at the host and is excluded above, so the plugin skips it.
# The second image is the current branch. That checkout has a real .git
# directory, and the plugin writes git.properties itself. mvn clean deletes
# target/, so the stand-in has to live in src/main/resources.
for module in webapp cli; do
  printf 'git.branch=detached\ngit.commit.id=%s\n' "$COMMIT" \
    > "$WORKTREE/$module/src/main/resources/git.properties"
done

echo "Building ${COMMIT} (latest migration V${TARGET}) as ${IMAGE}"
# linux/amd64 so the V66 frontend can use the prebuilt node-sass binary.
DOCKER_BUILDKIT=1 docker build --platform linux/amd64 -f docker/Dockerfile-bk21 -t "$IMAGE" "$WORKTREE"

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
docker/smoke/mojito-local push -r "$REPO" -s /opt/smoke-source
docker/smoke/mojito-local import -r "$REPO" -s /opt/smoke-source -t /opt/smoke-import
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
