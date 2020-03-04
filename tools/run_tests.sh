#!/bin/bash
# Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.

echo "Usage: "
cd ..

if [ -z "$1" ]; then
  echo "ERROR: Please specify a target platform: linux32 | linux64 | macos"
else
  if [ -z "$2" ]; then
    echo "ERROR: Please specify a build type: Debug or Release"
  else
    export OUT_PATH="./out/$1"

    #export LIB_PATH=$(readlink -f "./jcef_build/native/$2")
    export LIB_PATH=`pwd`/jcef_build/native/$2
    if [ ! -d "$LIB_PATH" ]; then
      echo "ERROR: Native build output path does not exist"
      exit 1
    fi

    if [ ! -d $OUT_PATH ]; then
      export OUT_PATH=$LIB_PATH
    fi

    export CLS_PATH="./third_party/jogamp/jar/*:$OUT_PATH"

    # Necessary for jcef_helper to find libcef.so.
    #export LD_LIBRARY_PATH=$LIB_PATH

    # Remove the first two params ($1 and $2) and pass the rest to java.
    shift
    shift

    #LD_PRELOAD=$LIB_PATH/libcef.so java -Djava.library.path=$LIB_PATH -jar ./third_party/junit/junit-platform-console-standalone-*.jar -cp $OUT_PATH --select-package tests.junittests "$@"

    echo "TEST_JAVA_HOME=$TEST_JAVA_HOME"
    if [ ! -d "$TEST_JAVA_HOME" ]; then
      echo "ERROR: Please set TEST_JAVA_HOME to existing jbr dir"
      exit 1
    fi

    CMD="$TEST_JAVA_HOME/bin/java -cp ./third_party/junit/junit-platform-console-standalone-1.4.2.jar:$OUT_PATH/jcef-tests.jar \
          org.junit.platform.console.ConsoleLauncher --select-package tests.junittests \
            -details=verbose --config=debugPrint=true $@"
    echo $CMD
    $CMD
    exit_status=$?
    echo "Test run result: $exit_status"
  fi
fi

cd tools

exit $exit_status
