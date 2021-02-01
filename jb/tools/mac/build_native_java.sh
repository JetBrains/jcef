# Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

source set_env.sh || exit 1

# shellcheck source=../common/common.sh
source "$JB_TOOLS_DIR"/common/common.sh || exit 1

OUT_DIR=$JCEF_ROOT_DIR/jcef_build
OUT_JAVA_DIR=$JCEF_ROOT_DIR/out

if [ "$1" == "clean" ]; then
    echo "*** delete $OUT_DIR..."
    rm -rf "$OUT_DIR"
    echo "*** delete $OUT_JAVA_DIR..."
    rm -rf "$OUT_JAVA_DIR"
    exit 0
fi
mkdir -p "$OUT_DIR" || do_fail
mkdir -p "$OUT_JAVA_DIR" || do_fail

case "$2" in
    "arm64")
        export TARGET_ARCH="arm64"
        ;;
    "x86_64")
        export TARGET_ARCH="x86_64"
        ;;
esac

echo "*** run cmake [TARGET=$TARGET_ARCH]..."
cd "$OUT_DIR" || do_fail
cmake -G "Xcode" -DPROJECT_ARCH="$TARGET_ARCH" .. || do_fail

echo "*** run xcodebuild..."
xcodebuild -configuration Release || do_fail

echo "*** change @rpath in libjcef.dylib..."
cd "$JCEF_ROOT_DIR"/jcef_build/native/Release || do_fail
install_name_tool -change @rpath/libjvm.dylib @loader_path/server/libjvm.dylib libjcef.dylib || do_fail
install_name_tool -change @rpath/libjawt.dylib @loader_path/libjawt.dylib libjcef.dylib || do_fail

if [ "$TARGET_ARCH" == "arm64" ]; then
    JNF_RPATH=$(otool -L libjcef.dylib | grep JavaNativeFoundation | awk '{print $1}')
    test -z "$JNF_RPATH" || install_name_tool -change "$JNF_RPATH" @loader_path/../../Frameworks/JavaNativeFoundation.framework/JavaNativeFoundation libjcef.dylib || do_fail
fi

cp libjcef.dylib jcef_app.app/Contents/Java || do_fail

cd "$JB_TOOLS_OS_DIR" || do_fail
