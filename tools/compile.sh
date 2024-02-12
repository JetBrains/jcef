#!/bin/bash
# Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.
set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "ERROR: Please specify a target platform: linux32 or linux64"
  exit 1
elif [ -z "${2:-}" ]; then
  echo "ERROR: Please specify a target configuration: Release or Debug"
  exit 1
else
  DIR="$JCEF_ROOT_DIR"
  export OUT_PATH="${DIR}/out/$1"

  JAVA_PATH="${DIR}/java"
  export CLS_PATH="${DIR}/third_party/jogamp/jar/*:${DIR}/third_party/junit/*:${JAVA_PATH}"

  if [ ! -d "$OUT_PATH" ]; then
    mkdir -p "$OUT_PATH"
  fi

  #$JAVA_HOME/bin/javac -Xdiags:verbose -cp "$CLS_PATH" -d "$OUT_PATH" "${JAVA_PATH}"/tests/detailed/*.java "${JAVA_PATH}"/tests/junittests/*.java "${JAVA_PATH}"/tests/simple/*.java "${JAVA_PATH}"/org/cef/*.java "${JAVA_PATH}"/org/cef/browser/*.java "${JAVA_PATH}"/org/cef/callback/*.java "${JAVA_PATH}"/org/cef/handler/*.java "${JAVA_PATH}"/org/cef/misc/*.java "${JAVA_PATH}"/org/cef/network/*.java
  cd "$DIR"
  ant -v jar tests-jar

  # Copy MANIFEST.MF
  if which rsync &>/dev/null; then
    rsync -a "${JAVA_PATH}"/manifest/MANIFEST.MF $OUT_PATH/manifest/
  else
    cp "${JAVA_PATH}"/manifest/MANIFEST.MF $OUT_PATH/manifest/
  fi

  # Copy resource files.
  if [ -d "${JAVA_PATH}"/tests/detailed/handler ]; then
    cp -f "${JAVA_PATH}"/tests/detailed/handler/*.html "$OUT_PATH/tests/detailed/handler"
    cp -f "${JAVA_PATH}"/tests/detailed/handler/*.png "$OUT_PATH/tests/detailed/handler"
  fi
fi
