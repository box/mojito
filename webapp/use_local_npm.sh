#!/usr/bin/env bash

# usage "source use_local_npm.sh"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export PATH="$DIR/node:$DIR/node/node_modules/npm/bin:$PATH"
