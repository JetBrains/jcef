#!/bin/bash
# Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.

cd ..

if [ -z "$1" ]; then
  echo "ERROR: Please specify a target platform: linux32 or linux64"
elif [ -z "$2" ]; then
  echo "ERROR: Please specify a target configuration: Release or Debug"
else
  export OUT_PATH="./out/$1"
  export OUT_NATIVE_PATH="./jcef_build/native/$2"
  export CLS_PATH="./third_party/jogamp/jar/*:./third_party/junit/*:./java"

  if [ ! -d "$OUT_PATH" ]; then
    mkdir -p "$OUT_PATH"
  fi

  #$JAVA_HOME/bin/javac -Xdiags:verbose -cp $CLS_PATH -d $OUT_PATH java/tests/detailed/*.java java/tests/junittests/*.java java/tests/simple/*.java java/org/cef/*.java java/org/cef/browser/*.java java/org/cef/callback/*.java java/org/cef/handler/*.java java/org/cef/misc/*.java java/org/cef/network/*.java
  ant -v modular-sdk

  # Copy resource files.
  cp -f ./java/tests/detailed/handler/*.html $OUT_PATH/tests/detailed/handler
  cp -f ./java/tests/detailed/handler/*.png $OUT_PATH/tests/detailed/handler
fi

cd tools

