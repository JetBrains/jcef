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
  echo "  JAVA_HOME          - Path to java home."
  echo "  ANT_HOME           - Path to 'ant' home, or if not set then 'ant' must be in PATH."
  echo "  JNF_FRAMEWORK_PATH - [optional] When no standard location is suitable."
  echo "  JNF_HEADERS_PATH   - [optional] When no standard location is suitable."
  echo "  CEF_BUILD_TYPE     - [optional] Build type(Debug or Release)"
  echo "  JCEF_CLEANUP_VCPKG - [optional] Cleanup the checkout dir before build"
  exit "$1"
}

function log() {
  echo
  echo "$(date +%FT%T%z)  " "$@"
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

HOMEBREW_NO_AUTO_UPDATE=1 brew install bison
brew link bison --force
HOMEBREW_NO_AUTO_UPDATE=1 brew install pkg-config

CEF_BUILD_TYPE=${CEF_BUILD_TYPE:-Release}
export CEF_BUILD_TYPE

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

if [ "${JCEF_CLEANUP_VCPKG:-0}" != "0" ] || [ -n "${TEAMCITY_VERSION:-}" ]; then
  echo "Cleaning up the vcpkg checkout directory..."
  cd "$script_dir/../../../"
  git submodule foreach --recursive git clean -xfdf
  git submodule foreach --recursive git reset --hard
  git submodule update --init --recursive
  echo "Removing thrift compiler..."
  rm -rf ./third_party/thrift_compiler
  cd -
fi

PATH="$script_dir:$PATH"
export PATH

log "*** BUILD NATIVE & JAVA ***"
bash "$script_dir/build_native_java.sh" $CLEAN

log "*** CREATE BUNDLE ***"
bash "$script_dir/create_bundle.sh" $CLEAN

log "*** BUILD SUCCESSFUL"
