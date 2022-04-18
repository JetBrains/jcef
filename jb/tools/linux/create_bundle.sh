#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

function clean {
  if test -f "$JCEF_ROOT_DIR/$1" || test -f "$JCEF_ROOT_DIR/$1.tar.gz"; then
    echo "*** delete $1..."
    rm -rf "${JCEF_ROOT_DIR:?}/$1"
    rm -f "${JCEF_ROOT_DIR:?}/$1.tar.gz"
  fi
}

case "$TARGET_ARCH" in
arm64) ARTIFACT=jcef_linux_aarch64 ;;
x86_64) ARTIFACT=jcef_linux_x64 ;;
*) echo "Incorrect TARGET_ARCH: $TARGET_ARCH" && exit 1 ;;
esac

clean jcef_linux_aarch64
clean jcef_linux_x64

if [ "${1:-}" == "clean" ]; then
  exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

if [ "${TARGET_ARCH}" == "x86_64" ]; then
  echo "*** bundle jogl and gluegen..."
  bash "$JB_TOOLS_DIR"/common/bundle_jogl_gluegen.sh
fi

echo "*** copy jcef binaries..."
mkdir "$ARTIFACT"
cp -R jcef_build/native/Release/* "$ARTIFACT"/
cp -R "$MODULAR_SDK_DIR" "$ARTIFACT"/

echo "*** copy patched libcef.so..."
if [ -z "${PATCHED_LIBCEF_DIR:-}" ]; then
  echo "warning: PATCHED_LIBCEF_DIR is not set. Current dir will be used: $(pwd)"
  PATCHED_LIBCEF_DIR="$(pwd)"
fi
if [ ! -f "$PATCHED_LIBCEF_DIR/libcef.so" ]; then
  echo "warning: $PATCHED_LIBCEF_DIR/libcef.so does not exist. Stock libcef.so will be used"
  PATCHED_LIBCEF_DIR="$JCEF_ROOT_DIR"/jcef_build/native/Release
fi
cp "$PATCHED_LIBCEF_DIR"/libcef.so "$ARTIFACT"/

echo "*** strip..."
# shellcheck disable=SC2038
find "$ARTIFACT" -name "*.so" | xargs strip -x
strip -x "$ARTIFACT"/chrome-sandbox
strip -x "$ARTIFACT"/jcef_helper

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz" || exit 1

echo "*** SUCCESSFUL"
