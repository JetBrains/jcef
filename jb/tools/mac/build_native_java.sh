#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

OUT_DIR="$JCEF_ROOT_DIR/jcef_build"
OUT_JAVA_DIR="$JCEF_ROOT_DIR/out"

if [ "${1:-}" == "clean" ]; then
  echo "*** delete $OUT_DIR..."
  rm -rf "$OUT_DIR"
  echo "*** delete $OUT_JAVA_DIR..."
  rm -rf "$OUT_JAVA_DIR"
  exit 0
fi
mkdir -p "$OUT_DIR"
mkdir -p "$OUT_JAVA_DIR"

echo "*** run cmake [TARGET=$TARGET_ARCH]..."
cd "$OUT_DIR" || exit 1
cmake -G "Xcode" -DPROJECT_ARCH="$TARGET_ARCH" ..

echo "*** run xcodebuild..."
xcodebuild -configuration ${CEF_BUILD_TYPE}

echo "*** change @rpath in libjcef.dylib..."
cd "$OUT_DIR"/native/${CEF_BUILD_TYPE} || exit 1

cp libjcef.dylib jcef_app.app/Contents/Java
