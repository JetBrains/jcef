# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if [ -z "$1" ] || [ "$1" == "help" ]; then
    echo "build.sh [option]"
    echo "Options:"
    echo "  help        - Print this help."
    echo "  all [arch]  - Build all the artifacts. Optional 'arch' defines the target CPU: x86_64 (default) or arm64."
    echo "  clean       - Clean all the artifacts."
    echo "Environment variables:"
    echo "  JAVA_HOME           - Path to java home."
    echo "  ANT_HOME            - Path to 'ant' home, or if not set then 'ant' must be in PATH."
    echo "  JNF_FRAMEWORK_PATH  - [optional] When no standard location is suitable."
    echo "  JNF_HEADERS_PATH    - [optional] When no standard location is suitable."
    exit 0
fi

if [ "$1" == "clean" ]; then
    CLEAN="clean"
    export TARGET_ARCH=
else
    if [ ! "$1" == "all" ]; then
        echo "error: wrong option, use 'help'"
        exit 1
    fi
    CLEAN=
    export TARGET_ARCH=$2
    test -z "$TARGET_ARCH" && export TARGET_ARCH="x86_64"
fi

if [ ! -d "../../../jb/tools/mac" ]; then
    echo "error: not in 'jb/tools/mac' dir"
    exit 1
fi

PATH=$(pwd):$PATH
export PATH

echo -e "\n*** BUILD NATIVE & JAVA ***\n"
bash build_native_java.sh "$CLEAN" || exit 1

echo -e "\n*** CREATE BUNDLE ***\n"
bash create_bundle.sh "$CLEAN" || exit 1

echo "*** BUILD SUCCESSFUL"
cd "$JB_TOOLS_OS_DIR" || exit 1
