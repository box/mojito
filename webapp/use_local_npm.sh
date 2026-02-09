#!/usr/bin/env bash

# Usage: source use_local_npm.sh
# Works in bash and zsh, ensures PATH includes the Maven-managed Node/npm under webapp/node

_mojito_use_local_npm_resolve_script() {
  if [ -n "${BASH_SOURCE:-}" ]; then
    printf '%s' "${BASH_SOURCE[0]}"
    return
  fi

  if [ -n "${ZSH_VERSION:-}" ]; then
    # zsh-only expansion to get the current script path even when sourced
    printf '%s' "${(%):-%x}"
    return
  fi

  # Fallback (may be incorrect when sourcing from other shells)
  printf '%s' "$0"
}

_mojito_use_local_npm_script="$(_mojito_use_local_npm_resolve_script)"
_mojito_use_local_npm_dir="$( cd "$( dirname "${_mojito_use_local_npm_script}" )" && pwd )"

export PATH="$_mojito_use_local_npm_dir/node:$_mojito_use_local_npm_dir/node/node_modules/npm/bin:$PATH"

unset -f _mojito_use_local_npm_resolve_script
unset _mojito_use_local_npm_script
unset _mojito_use_local_npm_dir
