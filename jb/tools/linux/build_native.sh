#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -xeuo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

OUT_DIR="$JCEF_ROOT_DIR/jcef_build"

if [ "${1:-}" == "clean" ]; then
  echo "*** delete $OUT_DIR..."
  rm -rf "$OUT_DIR"
  exit 0
fi
rm -rf "$OUT_DIR" && mkdir "$OUT_DIR"

cd "$JCEF_ROOT_DIR" || exit 1

# workaround python failure in docker
git checkout tools/make_version_header.py

echo "*** run cmake [TARGET=$TARGET_ARCH]..."
cd "$OUT_DIR" || exit 1

cmake_executable="cmake"
if [ -n "${ALT_JCEF_CMAKE:-}" ]; then
  echo "Use alt cmake $ALT_JCEF_CMAKE"
  cmake_executable=$ALT_JCEF_CMAKE
fi

additional_cmake=""
if [ -n "${CEF_VERSION:-}" ]; then
  additional_cmake="$additional_cmake -DCEF_VERSION=$CEF_VERSION"
fi

if [ -n "${CEF_DONT_DOWNLOAD:-}" ]; then
  additional_cmake="$additional_cmake -DCEF_DONT_DOWNLOAD=$CEF_DONT_DOWNLOAD"
fi

$cmake_executable -G "Unix Makefiles" -DPROJECT_ARCH="$TARGET_ARCH" -DCMAKE_BUILD_TYPE=Release $additional_cmake ..

echo "*** run make..."
make -j4
