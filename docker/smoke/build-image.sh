#!/usr/bin/env bash
# Build the local smoke webapp and record which migration sources went into it.
#
#   docker/smoke/build-image.sh
#
# A Docker image is a snapshot. Java migrations are compiled into .class files,
# so a later check cannot compare those .java files with the running jar.
# This script records a fingerprint of the SQL and Java migration sources and
# the ID of the image just built. docker/smoke/check.sh requires the running
# container to be that image and the sources to still have that fingerprint.
#
# The record is docker/.data/webapp-build.txt. Recipes delete docker/.data/db
# only, so replacing the database does not throw away this record.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=lib.sh
source "$ROOT/docker/smoke/lib.sh"

COMPOSE_FILE="$ROOT/docker/docker-compose-mysql-smoke.yml"
STAMP_DIR="$ROOT/docker/.data"
STAMP="$STAMP_DIR/webapp-build.txt"
IMAGE_NAME="mojito-mysql-smoke-webapp:latest"

require_sha256

before="$(migration_manifest "$ROOT")"
docker compose -f "$COMPOSE_FILE" build webapp
after="$(migration_manifest "$ROOT")"

if [[ "$before" != "$after" ]]; then
  echo "Migration files changed while the image was building." >&2
  echo "The build record was not updated. Run docker/smoke/build-image.sh again without changing migration files during the build." >&2
  exit 1
fi

image_id="$(docker image inspect "$IMAGE_NAME" --format '{{.Id}}')"
fingerprint="$(migration_fingerprint "$after")"

mkdir -p "$STAMP_DIR"
temporary="$(mktemp "$STAMP_DIR/webapp-build.txt.tmp.XXXXXX")"
{
  printf 'image_id=%s\n' "$image_id"
  printf 'fingerprint=%s\n' "$fingerprint"
  printf '%s\n' "$after"
} > "$temporary"
mv "$temporary" "$STAMP"

echo "Recorded $IMAGE_NAME ($image_id)"
echo "Migration fingerprint $fingerprint"
