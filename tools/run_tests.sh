#!/bin/bash
# Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.

if [ -z "$1" ]; then
  echo "ERROR: Please specify a target platform: linux32 | linux64 | macos"
else
  if [ -z "$2" ]; then
    echo "ERROR: Please specify a build type: Debug or Release"
  else
    DIR="$( cd "$( dirname "$0" )" && cd .. && pwd )"
    OUT_PATH="${DIR}/out/$1"

    export LIB_PATH=$(readlink -f "./jcef_build/native/$2")
    if [ ! -d "$LIB_PATH" ]; then
      echo "ERROR: Native build output path does not exist"
      exit 1
    fi

    if [ ! -d $OUT_PATH ]; then
      export OUT_PATH=$LIB_PATH
    fi

    export CLS_PATH="${DIR}/third_party/jogamp/jar/*:$OUT_PATH"

    # Necessary for jcef_helper to find libcef.so.
    #if [ -n "$LD_LIBRARY_PATH" ]; then
    #  LD_LIBRARY_PATH="$LIB_PATH:${LD_LIBRARY_PATH}"
    #else
    #  LD_LIBRARY_PATH="$LIB_PATH"
    #fi
    #export LD_LIBRARY_PATH

    # Remove the first two params ($1 and $2) and pass the rest to java.
    shift
    shift

    #LD_PRELOAD=libcef.so java -Djava.library.path="$LIB_PATH" -jar "${DIR}"/third_party/junit/junit-platform-console-standalone-*.jar -cp "$OUT_PATH" --select-package tests.junittests "$@"

    echo "TEST_JAVA_HOME=$TEST_JAVA_HOME"
    if [ ! -d "$TEST_JAVA_HOME" ]; then
      echo "ERROR: Please set TEST_JAVA_HOME to existing jbr dir"
      exit 1
    fi

    CMD="$TEST_JAVA_HOME/bin/java -cp ./third_party/junit/junit-platform-console-standalone-1.10.0.jar:$OUT_PATH/jcef-tests.jar \
          org.junit.platform.console.ConsoleLauncher --select-package tests.junittests \
            --details=verbose --config=debugPrint=true $@"
    echo $CMD
    $CMD
    exit_status=$?
    echo "Test run result: $exit_status"
  fi
fi

cd tools

exit $exit_status
