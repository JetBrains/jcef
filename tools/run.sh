#!/bin/bash
# Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file.

cd ..

if [ -z "$1" ]; then
  echo "ERROR: Please specify a target platform: linux32 | linux64 | macosx"
else
  if [ -z "$2" ]; then
    echo "ERROR: Please specify a build type: Debug or Release"
  elif [ -z "$3" ]; then
    echo "ERROR: Please specify a run type: detailed or simple"
  else
    export OUT_PATH="./out/$1"

    export LIB_PATH=$(readlink -f "./jcef_build/native/$2")
    export LIB_PATH=`pwd`/jcef_build/native/$2
    if [ ! -d "$LIB_PATH" ]; then
      echo "ERROR: Native build output path does not exist"
      exit 1
    fi

    if [ ! -d $OUT_PATH ]; then
      export OUT_PATH=$LIB_PATH
    fi

    export CLS_PATH="./third_party/jogamp/jar/*:$OUT_PATH"
    export RUN_TYPE="$3"

    # Necessary for jcef_helper to find libcef.so.
    #export LD_LIBRARY_PATH=$LIB_PATH

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

    CMD="$JAVA_HOME/bin/java -cp $OUT_PATH/jcef-tests.jar tests.$RUN_TYPE.MainFrame $@"
    echo $CMD
    $CMD
    exit_status=$?
    echo "Test run result: $exit_status"
  fi
fi

cd tools

exit $exit_status
