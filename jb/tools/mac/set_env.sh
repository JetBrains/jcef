# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ ! -f "./set_env.sh" ]; then
    echo "error: not in jb tools mac dir"
    exit 1
fi

cd ../../..

JCEF_ROOT_DIR=$(pwd)
export JCEF_ROOT_DIR
export JB_TOOLS_DIR=$JCEF_ROOT_DIR/jb/tools
export JB_TOOLS_OS_DIR=$JB_TOOLS_DIR/mac
export MODULAR_SDK_DIR="$JCEF_ROOT_DIR"/jcef_build/native/Release/modular-sdk
export OS=macosx
export ARCH=universal

cd "$JB_TOOLS_OS_DIR" || exit 1

if [ -z "$JDK_11" ]; then
    echo "error: JDK_11 is not set"
    exit 1
fi
export JAVA_HOME=$JDK_11
echo "JAVA_HOME=$JAVA_HOME"

# shellcheck disable=SC2230
if ! which ant
then
    if [ -z "$ANT_HOME" ]; then
        echo "error: ANT_HOME is not set"
        exit 1
    fi
    export PATH=$ANT_HOME/bin:$PATH
    echo "ANT_HOME=$ANT_HOME"
fi
