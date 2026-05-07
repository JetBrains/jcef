#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

function clean {
  if test -f "$JCEF_ROOT_DIR/$1" || test -f "$JCEF_ROOT_DIR/$1.tar.gz" || test -d "$JCEF_ROOT_DIR/$1"; then
    echo "*** delete $1..."
    rm -rf "${JCEF_ROOT_DIR:?}/$1"
    rm -f "${JCEF_ROOT_DIR:?}/$1.tar.gz"
  fi
  rm -f "${JCEF_ROOT_DIR:?}/jcef.version"
}

case "$TARGET_ARCH" in
arm64)
  ARTIFACT=jcef_linux_aarch64
  ARTIFACT_SERVER=cef_server_linux_aarch64
  ARTIFACT_NATIVE_BUNDLE=jcef_native_bundle_linux_aarch64
  ;;
x86_64)
  ARTIFACT=jcef_linux_x64
  ARTIFACT_SERVER=cef_server_linux_x64
  ARTIFACT_NATIVE_BUNDLE=jcef_native_bundle_linux_x86_64
  ;;
*) echo "Incorrect TARGET_ARCH: $TARGET_ARCH" && exit 1 ;;
esac

clean jcef_linux_aarch64
clean jcef_linux_x64
clean jcef_native_bundle_linux_arm64
clean jcef_native_bundle_linux_x86_64
clean "$ARTIFACT_NATIVE_BUNDLE"

if [ "${1:-}" == "clean" ]; then
  exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

bash "$JB_TOOLS_DIR"/common/create_modules.sh

echo "*** copy jcef binaries..."
rm -rf "$ARTIFACT" && mkdir "$ARTIFACT"
mv jmods "$ARTIFACT"/

# create jcef.version file
bash "$JB_TOOLS_DIR"/common/create_version_file.sh $ARTIFACT
cp "$ARTIFACT/jcef.version" .

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz"

if [ -n "${JCEF_BUILD_ONLY_IN_PROCESS:-}" ]; then
  echo "*** skip bundling of out-of-process JCEF."
else
  echo "*** create cef_server archive..."
  tar -cvzf "$ARTIFACT_SERVER.tar.gz" -C "cef_server" $(ls "cef_server")
  rm -rf "cef_server"
  ls -lah "$ARTIFACT_SERVER.tar.gz"

  echo "*** create standalone native bundle..."
  rm -rf "$ARTIFACT_NATIVE_BUNDLE" && mkdir -p "$ARTIFACT_NATIVE_BUNDLE"/jcef
  cp -R "$OUT_REMOTE_DIR"/* "$ARTIFACT_NATIVE_BUNDLE"/jcef/
  cp "$OUT_NATIVE_DIR"/libjcef.so "$ARTIFACT_NATIVE_BUNDLE"/jcef/
  cp "$OUT_NATIVE_DIR"/jcef_helper "$ARTIFACT_NATIVE_BUNDLE"/jcef/
  cp "jcef.version" "$ARTIFACT_NATIVE_BUNDLE"/jcef/

  echo "*** create standalone native bundle archive..."
  tar -cvzf "$ARTIFACT_NATIVE_BUNDLE.tar.gz" -C "$ARTIFACT_NATIVE_BUNDLE" jcef
  rm -rf "$ARTIFACT_NATIVE_BUNDLE"
  ls -lah "$ARTIFACT_NATIVE_BUNDLE.tar.gz"
fi

cp "$OUT_CLS_DIR"/jcef-tests.jar .
rm -f "jcef.version"

echo "*** SUCCESSFUL"
