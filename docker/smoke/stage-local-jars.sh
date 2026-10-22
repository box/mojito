#!/usr/bin/env bash
# Copy host-built Mojito jars onto stable names for the edit-loop mount.
#
#   mvn -DskipTests package
#   docker/smoke/stage-local-jars.sh
#
# Maven writes mojito-webapp-*-exec.jar and mojito-cli-*-exec.jar. Compose
# cannot mount a glob. The webapp process keeps the jar it opened at start,
# so recreate the webapp after this copy. See docker/readme.md.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEST="$ROOT/docker/local-jars"

copy_one() {
  local dir="$1"
  local pattern="$2"
  local dest_name="$3"
  local matches=()
  local jar

  while IFS= read -r -d '' jar; do
    matches+=("$jar")
  done < <(find "$dir" -maxdepth 1 -type f -name "$pattern" -print0)

  if [[ ${#matches[@]} -ne 1 ]]; then
    echo "Expected one $pattern under $dir, found ${#matches[@]}." >&2
    echo "Build on the host first: mvn -DskipTests package" >&2
    exit 1
  fi

  cp "${matches[0]}" "$DEST/$dest_name"
}

mkdir -p "$DEST"
copy_one "$ROOT/webapp/target" 'mojito-webapp-*-exec.jar' 'mojito-webapp.jar'
copy_one "$ROOT/cli/target" 'mojito-cli-*-exec.jar' 'mojito-cli.jar'

echo "Staged jars in docker/local-jars."
echo "Recreate the webapp with docker/docker-compose-mysql-smoke.local-jars.yml so the container opens these files."
