# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ ! -f "./set_env.sh" ]; then
    echo "error: not in jb tools linux dir"
    exit 1
fi

cd ../../..

JCEF_ROOT_DIR=$(pwd)
export JCEF_ROOT_DIR
export JB_TOOLS_DIR=$JCEF_ROOT_DIR/jb/tools
export JB_TOOLS_OS_DIR=$JB_TOOLS_DIR/linux
export MODULAR_SDK_DIR="$JCEF_ROOT_DIR"/out/linux64/modular-sdk
export OS=linux
export DEPS_ARCH=amd64

cd "$JB_TOOLS_OS_DIR" || exit 1

if [ -z "$JDK_11" ]; then
    if [ -d "jdk11" ]; then
        # set on the TeamCity agent
        JDK_11="$JCEF_ROOT_DIR"/jdk11
    else
        echo "error: JDK_11 is not set"
        exit 1
    fi
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

