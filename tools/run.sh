#!/bin/bash
# Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.

if [ -z "$1" ]; then
  echo "ERROR: Please specify a target platform: linux32 | linux64 | macos"
else
  if [ -z "$2" ]; then
    echo "ERROR: Please specify a build type: Debug or Release"
  elif [ -z "$3" ]; then
    echo "ERROR: Please specify a run type: detailed or simple"
  else
    DIR="$( cd "$( dirname "$0" )" && pwd )"
    OUT_PATH="${DIR}/out/$1"

    export LIB_PATH="${DIR}/jcef_build/native/$2"
    if [ ! -d "$LIB_PATH" ]; then
      echo "ERROR: Native build output path does not exist"
      exit 1
    fi

    if [ ! -d $OUT_PATH ]; then
      export OUT_PATH=$LIB_PATH
    fi

    # Necessary for libjcef.so to find libjawt.so.
    JAVA_PATH="$(readlink -f $(which java))"
    JAVA_LIB_PATH="$(dirname ${JAVA_PATH})/../lib"
    if [ ! -d "$JAVA_LIB_PATH" ]; then
      echo "ERROR: Java lib path does not exist"
      exit 1
    fi

    LIB_PATH="${LIB_PATH}:${JAVA_LIB_PATH}"

    export CLS_PATH="${DIR}/third_party/jogamp/jar/*:$OUT_PATH"
    export RUN_TYPE="$3"

    # Necessary for jcef_helper to find libcef.so.
    if [ -n "$LD_LIBRARY_PATH" ]; then
      LD_LIBRARY_PATH="$LIB_PATH:${LD_LIBRARY_PATH}"
    else
      LD_LIBRARY_PATH="$LIB_PATH"
    fi
    export LD_LIBRARY_PATH

    # Remove the first three params ($1, $2 and $3) and pass the rest to java.
    shift
    shift
    shift

    #LD_PRELOAD=$LIB_PATH/libcef.so $JAVA_HOME/bin/java -cp "$CLS_PATH" -Djava.library.path=$LIB_PATH --add-exports java.desktop/sun.lwawt.macosx=jcef --add-exports java.desktop/sun.lwawt=jcef --add-exports java.desktop/sun.awt=jcef tests.$RUN_TYPE.MainFrame "$@"
    #$JAVA_HOME/bin/java -cp $OUT_PATH --add-exports java.desktop/sun.lwawt.macosx=jcef --add-exports java.desktop/sun.lwawt=jcef --add-exports java.desktop/sun.awt=jcef tests.$RUN_TYPE.MainFrame "$@"

    echo "TEST_JAVA_HOME=$TEST_JAVA_HOME"
    if [ ! -d "$TEST_JAVA_HOME" ]; then
      echo "ERROR: Please set TEST_JAVA_HOME to existing jbr dir"
      exit 1
    fi

    CMD="$TEST_JAVA_HOME/bin/java -cp $OUT_PATH/jcef-tests.jar tests.$RUN_TYPE.MainFrame $@"
    echo $CMD
    $CMD
    exit_status=$?
    echo "Test run result: $exit_status"
  fi
fi


exit $exit_status
