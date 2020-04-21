# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ -z "$1" ] || [ "$1" == "help" ]; then
    echo "build.sh [option]"
    echo "Options:"
    echo "  help        - Print this help."
    echo "  all         - Build all the artifacts."
    echo "  clear       - Clear all the artifacts."
    echo "Environment variables:"
    echo "  JDK_11      - Path to OpenJDK 11 home."
    echo "  ANT_HOME    - Path to 'ant' home, or if not set then 'ant' must be in PATH."
    exit 0
fi

if [ "$1" == "clear" ]; then
    CLEAR="clear"
else
    if [ ! "$1" == "all" ]; then
        echo "error: wrong option, use 'help'"
        exit 1
    fi
    CLEAR=
fi

if [ ! -d "../../../jb/tools/mac" ]; then
    echo "error: not in 'jb/tools/mac' dir"
    exit 1
fi

PATH=$(pwd):$PATH
export PATH

echo -e "\n*** BUILD NATIVE & JAVA ***\n"
bash build_native_java.sh $CLEAR || exit 1

echo -e "\n*** CREATE BUNDLE ***\n"
bash create_bundle.sh $CLEAR || exit 1
