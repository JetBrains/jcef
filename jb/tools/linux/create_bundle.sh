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
arm64)
  ARTIFACT=jcef_linux_aarch64
  ARTIFACT_SERVER=cef_server_linux_aarch64
  ;;
x86_64)
  ARTIFACT=jcef_linux_x64
  ARTIFACT_SERVER=cef_server_linux_x64
  ;;
*) echo "Incorrect TARGET_ARCH: $TARGET_ARCH" && exit 1 ;;
esac

clean jcef_linux_aarch64
clean jcef_linux_x64

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

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz"

echo "*** create cef_server archive..."
cp third_party/thrift/libthrift-0.19.0.jar cef_server
cp third_party/slf4j/slf4j-api-2.0.0.jar cef_server
tar -cvzf "$ARTIFACT_SERVER.tar.gz" -C "cef_server" $(ls "cef_server")
rm -rf "cef_server"
ls -lah "$ARTIFACT_SERVER.tar.gz"

cp "$OUT_CLS_DIR"/jcef-tests.jar .

echo "*** SUCCESSFUL"
