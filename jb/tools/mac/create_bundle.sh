#!/bin/bash
# Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "$0")" &>/dev/null && pwd)

source "$script_dir/set_env.sh"

function clean {
  if [ "$1" == "legacy" ]; then
    __artefact=jcef_mac
  else
    __artefact=jcef_mac_$1
  fi
  if test -f "$JCEF_ROOT_DIR/$__artefact" || test -f "$JCEF_ROOT_DIR/$__artefact.tar.gz"; then
    echo "*** delete $__artefact..."
    rm -rf "${JCEF_ROOT_DIR:?}/$__artefact"
    rm -f "${JCEF_ROOT_DIR:?}/$__artefact.tar.gz"
  fi
}
export -f clean

RELEASE_PATH="$JCEF_ROOT_DIR"/jcef_build/native/${CEF_BUILD_TYPE}
ARTIFACT=jcef_mac_${TARGET_ARCH}
ARTIFACT_SERVER=cef_server_mac_${TARGET_ARCH}

clean arm64
clean x86_64
clean universal
clean legacy

if [ "${1:-}" == "clean" ]; then
  exit 0
fi

cd "$JCEF_ROOT_DIR" || exit 1

echo "*** create modules..."
bash "$JB_TOOLS_DIR"/common/create_modules.sh

echo "*** copy jcef binaries..."
rm -rf "$ARTIFACT" && mkdir "$ARTIFACT"
mv jmods "$ARTIFACT"/
cp -R "$RELEASE_PATH"/jcef_app.app/Contents/Frameworks "$ARTIFACT"/
cp -R "$RELEASE_PATH"/../../remote/"$CEF_BUILD_TYPE"/cef_server.app "$ARTIFACT"/Frameworks/

echo "*** copy cef_server binaries..."
rm -rf "$ARTIFACT_SERVER" && mkdir "$ARTIFACT_SERVER"
cp -r "$RELEASE_PATH"/../../remote/"$CEF_BUILD_TYPE"/cef_server.app "$ARTIFACT_SERVER/"
cp "$RELEASE_PATH"/../../remote/"$CEF_BUILD_TYPE"/libshared_mem_helper.dylib "$ARTIFACT_SERVER"
cp -r "${RELEASE_PATH}/jcef_app.app/Contents/Frameworks/Chromium Embedded Framework.framework" "$ARTIFACT_SERVER"/cef_server.app/Contents/Frameworks

echo "*** create jcef.version file..."
bash "$JB_TOOLS_DIR"/common/create_version_file.sh $ARTIFACT

echo "*** create archive..."
# shellcheck disable=SC2046
tar -cvzf "$ARTIFACT.tar.gz" -C "$ARTIFACT" $(ls "$ARTIFACT")
rm -rf "$ARTIFACT"
ls -lah "$ARTIFACT.tar.gz"

echo "*** create cef_server archive..."
cp third_party/thrift/libthrift-0.19.0.jar $ARTIFACT_SERVER
cp third_party/slf4j/slf4j-api-2.0.0.jar $ARTIFACT_SERVER
tar -cvzf "$ARTIFACT_SERVER.tar.gz" -C "$ARTIFACT_SERVER" $(ls "$ARTIFACT_SERVER")
rm -rf "$ARTIFACT_SERVER"
ls -lah "$ARTIFACT_SERVER.tar.gz"

cp "$OUT_CLS_DIR"/jcef-tests.jar .

if [ "$TARGET_ARCH" == "x86_64" ]; then
  # preserve legacy artefact name
  cp "$ARTIFACT.tar.gz" jcef_mac.tar.gz
fi

echo "*** SUCCESSFUL"
