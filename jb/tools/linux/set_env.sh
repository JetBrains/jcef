# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

export JB_TOOLS_LINUX_DIR=jb/tools/linux
export JCEF_ROOT_DIR=../../..

if [ ! -f "./set_env.sh" ]; then
    echo "error: not in <$JB_TOOLS_LINUX_DIR> dir"
    exit 1
fi

if [ -z "$ALT_JAVA_HOME" ]; then
    if [ ! -d "$JCEF_ROOT_DIR/jbrsdk" ]; then
        echo "error: <$JCEF_ROOT_DIR/jbrsdk> dir does not exist and ALT_JAVA_HOME is not set"
        exit 1
    fi
    export JAVA_HOME=jbrsdk
else
    export JAVA_HOME=$ALT_JAVA_HOME
fi

echo "JAVA_HOME=$JAVA_HOME"
