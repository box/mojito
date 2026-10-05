#!/usr/bin/env bash
# Shared helpers for the local MySQL smoke build and check.
# This file is meant to be sourced. It does not enable or disable shell options.

sha256_stdin() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum | awk '{print $1}'
  else
    shasum -a 256 | awk '{print $1}'
  fi
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

require_sha256() {
  if ! command -v sha256sum >/dev/null 2>&1 && ! command -v shasum >/dev/null 2>&1; then
    echo "Neither sha256sum nor shasum is installed; cannot verify the webapp image." >&2
    exit 1
  fi
}

# Sorted lines of "<sha256>  <path>", one per SQL or Java migration source file.
# The path is relative to the repository root. Command substitution removes the
# final newline; callers that hash this text must add that newline back.
migration_manifest() {
  local root="$1"
  local sql_dir="$root/webapp/src/main/resources/db/migration"
  local java_dir="$root/webapp/src/main/java/db/migration"
  local file relative_path checksum

  if [[ ! -d "$sql_dir" || ! -d "$java_dir" ]]; then
    echo "Migration directories not found under $root" >&2
    return 1
  fi

  while IFS= read -r -d '' file; do
    relative_path="${file#"$root"/}"
    checksum="$(sha256_file "$file")"
    printf '%s  %s\n' "$checksum" "$relative_path"
  done < <(find "$sql_dir" "$java_dir" -maxdepth 1 -type f \( -name 'V*.sql' -o -name 'V*.java' \) -print0) \
    | sort
}

migration_fingerprint() {
  printf '%s\n' "$1" | sha256_stdin
}
