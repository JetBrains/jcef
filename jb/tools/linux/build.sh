# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ -z "$1" ] || [ "$1" == "help" ]; then
    echo "build.sh [option]"
    echo "Options:"
    echo "  help                - Print this help."
    echo "  all                 - Build all the artifacts."
    echo "  clean               - Clean all the artifacts."
    echo "Environment variables:"
    echo "  JDK_11              - Path to OpenJDK 11 home."
    echo "  ANT_HOME            - Path to 'ant' home, or if not set then 'ant' must be in PATH."
    echo "  PATCHED_LIBCEF_DIR  - Path to the patched libcef.so dir, or if not set then the stock libcef.so is used."
    exit 0
fi

if [ "$1" == "clean" ]; then
    CLEAN="clean"
else
    if [ ! "$1" == "all" ]; then
        echo "error: wrong option, use 'help'"
        exit 1
    fi
    CLEAN=
fi

if [ ! -d "../../../jb/tools/linux" ]; then
    echo "error: not in 'jb/tools/linux' dir"
    exit 1
fi

PATH=$(pwd):$PATH

export PATHecho -e "\n*** BUILD NATIVE ***\n"
bash build_native.sh $CLEAN || exit 1

echo -e "\n*** BUILD JAVA ***\n"
bash build_java.sh $CLEAN || exit 1

echo -e "\n*** CREATE BUNDLE ***\n"
bash create_bundle.sh $CLEAN || exit 1
