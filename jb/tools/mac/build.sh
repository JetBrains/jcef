#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

function print_usage() {
  echo "build.sh [option]"
  echo "Options:"
  echo "  help        - Print this help."
  echo "  all [arch]  - Build all the artifacts. Optional 'arch' defines the target CPU: x86_64 (default) or arm64."
  echo "  clean       - Clean all the artifacts."
  echo "Environment variables:"
  echo "  JDK_11              - Path to OpenJDK 11 home."
  echo "  ANT_HOME            - Path to 'ant' home, or if not set then 'ant' must be in PATH."
  echo "  JNF_FRAMEWORK_PATH  - [optional] When no standard location is suitable."
  echo "  JNF_HEADERS_PATH    - [optional] When no standard location is suitable."
  exit "$1"
}

function log() {
  echo
  echo "$(date --rfc-3339=seconds)  " "$@"
  echo
}

case "${1:-help}" in
clean)
  CLEAN="clean"
  export TARGET_ARCH=
  ;;
all)
  CLEAN=""
  export TARGET_ARCH="${2:-x86_64}"
  ;;
help) print_usage 0 ;;
*) print_usage 1 ;;
esac

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

PATH="$script_dir:$PATH"
export PATH

log "*** BUILD NATIVE & JAVA ***"
bash "$script_dir/build_native_java.sh" $CLEAN

log "*** CREATE BUNDLE ***"
bash "$script_dir/create_bundle.sh" $CLEAN
